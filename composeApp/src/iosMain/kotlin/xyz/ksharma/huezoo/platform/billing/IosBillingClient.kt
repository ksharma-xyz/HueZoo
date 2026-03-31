package xyz.ksharma.huezoo.platform.billing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.StoreKit.SKPaymentTransactionState
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKRequest
import platform.darwin.NSObject
import kotlin.coroutines.resume

// SKErrorDomain code for user-cancelled payment (ObjC constant SKErrorPaymentCancelled = 2)
private const val SK_ERROR_PAYMENT_CANCELLED = 2

/**
 * StoreKit 1 implementation of [BillingClient] for iOS.
 *
 * Uses StoreKit 1 (ObjC API) because it is fully accessible from Kotlin/Native without
 * a Swift bridge. StoreKit 2 (Swift concurrency) requires additional interop setup.
 *
 * Lifecycle: registered as a Koin singleton — the transaction observer stays attached
 * to [SKPaymentQueue.defaultQueue] for the app's lifetime.
 *
 * ### What you need in App Store Connect before real data flows
 * 1. Register the app: App Store Connect → My Apps → + New App
 * 2. Create the IAP product: your app → Features → In-App Purchases →
 *    "+" → Non-Consumable → Product ID: [PRODUCT_UNLIMITED] → set price tier
 * 3. Submit the IAP for review along with your first app version
 * Once the product exists and the build is signed with your distribution certificate,
 * [queryPrice] returns the user's local currency string (€, ₹, £ etc.)
 */
class IosBillingClient : BillingClient {

    // ── Pending async operations ──────────────────────────────────────────────

    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null
    private var pendingRestore: CompletableDeferred<Set<String>>? = null

    /** Accumulates product IDs seen during a restoreCompletedTransactions call. */
    private val restoredIds = mutableSetOf<String>()

    // ── Transaction observer ──────────────────────────────────────────────────

    /**
     * Stored as a class property so it stays alive for the app's lifetime.
     * SKPaymentQueue retains observers, but we also hold a strong Kotlin reference
     * to be safe with Kotlin/Native's GC.
     */
    @Suppress("ObjectLiteralToLambda") // must be NSObject subclass, not a lambda
    private val observer: SKPaymentTransactionObserverProtocol =
        object : NSObject(), SKPaymentTransactionObserverProtocol {

            override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
                @Suppress("UNCHECKED_CAST")
                val transactions = updatedTransactions as List<SKPaymentTransaction>
                for (tx in transactions) {
                    when (tx.transactionState) {
                        SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                            queue.finishTransaction(tx)
                            val deferred = pendingPurchase.also { pendingPurchase = null }
                            deferred?.complete(PurchaseResult.Success)
                        }
                        SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                            // Collect IDs during restore; deferred completes in
                            // paymentQueueRestoreCompletedTransactionsFinished
                            tx.originalTransaction?.payment?.productIdentifier
                                ?.let { restoredIds += it }
                            queue.finishTransaction(tx)
                        }
                        SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                            val isCancelled =
                                tx.error?.code?.toInt() == SK_ERROR_PAYMENT_CANCELLED
                            queue.finishTransaction(tx)
                            val result = if (isCancelled) {
                                PurchaseResult.Cancelled
                            } else {
                                PurchaseResult.Error(
                                    tx.error?.localizedDescription ?: "Purchase failed",
                                )
                            }
                            val deferred = pendingPurchase.also { pendingPurchase = null }
                            deferred?.complete(result)
                        }
                        else -> Unit // Purchasing / Deferred — wait for next state update
                    }
                }
            }

            override fun paymentQueueRestoreCompletedTransactionsFinished(
                queue: SKPaymentQueue,
            ) {
                val ids = restoredIds.toSet().also { restoredIds.clear() }
                val deferred = pendingRestore.also { pendingRestore = null }
                deferred?.complete(ids)
            }

            override fun paymentQueue(
                queue: SKPaymentQueue,
                restoreCompletedTransactionsFailedWithError: NSError,
            ) {
                restoredIds.clear()
                val deferred = pendingRestore.also { pendingRestore = null }
                deferred?.complete(emptySet())
            }
        }

    init {
        SKPaymentQueue.defaultQueue().addTransactionObserver(observer)
    }

    // ── BillingClient ─────────────────────────────────────────────────────────

    override suspend fun queryPrice(productId: String): String? =
        fetchProduct(productId)?.let { product ->
            val formatter = NSNumberFormatter()
            formatter.numberStyle = NSNumberFormatterCurrencyStyle
            formatter.locale = product.priceLocale
            formatter.stringFromNumber(product.price)
        }

    override suspend fun purchase(productId: String): PurchaseResult {
        if (!SKPaymentQueue.canMakePayments()) {
            return PurchaseResult.Error("Purchases are not allowed on this device")
        }
        val product = fetchProduct(productId)
            ?: return PurchaseResult.Error("Product not found: $productId")
        val deferred = CompletableDeferred<PurchaseResult>()
        pendingPurchase = deferred
        SKPaymentQueue.defaultQueue().addPayment(SKPayment.paymentWithProduct(product))
        return deferred.await()
    }

    /**
     * Triggers [SKPaymentQueue.restoreCompletedTransactions] and returns true when the
     * user already owns [productId]. Shows the App Store sign-in dialog if the user isn't
     * signed in — this is expected UX for "Restore Purchases" flows.
     */
    override suspend fun isOwned(productId: String): Boolean {
        val deferred = CompletableDeferred<Set<String>>()
        pendingRestore = deferred
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
        return productId in deferred.await()
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Fetches a single [SKProduct] by [productId] via [SKProductsRequest].
     *
     * The [SelfRetainingProductsDelegate] pattern is required because
     * [SKProductsRequest.delegate] is a *weak* ObjC property — without an explicit
     * strong reference on the Kotlin side, the delegate object is eligible for GC
     * before the async callback fires.
     */
    private suspend fun fetchProduct(productId: String): SKProduct? =
        suspendCancellableCoroutine { cont ->
            val request = SKProductsRequest(productIdentifiers = setOf(productId))
            val delegate = SelfRetainingProductsDelegate { product ->
                if (cont.isActive) cont.resume(product)
            }
            request.delegate = delegate
            cont.invokeOnCancellation { request.cancel() }
            request.start()
        }
}

/**
 * [SKProductsRequest] delegate that retains itself until the callback fires.
 *
 * [SKProductsRequest.delegate] is declared `weak` in ObjC, so assigning a freshly
 * created Kotlin object would leave it with no strong owner — the GC could collect it
 * before the async response arrives. [selfRetain] holds a strong Kotlin reference that
 * is cleared in both callback paths, breaking the cycle and allowing normal collection
 * once the delegate's work is done.
 */
private class SelfRetainingProductsDelegate(
    private val onResult: (SKProduct?) -> Unit,
) : NSObject(), SKProductsRequestDelegateProtocol {

    @Suppress("unused")
    private var selfRetain: SelfRetainingProductsDelegate? = this

    override fun productsRequest(
        request: SKProductsRequest,
        didReceiveResponse: SKProductsResponse,
    ) {
        selfRetain = null
        @Suppress("UNCHECKED_CAST")
        onResult((didReceiveResponse.products as List<SKProduct>).firstOrNull())
    }

    override fun request(request: SKRequest, didFailWithError: NSError) {
        selfRetain = null
        onResult(null)
    }
}
