package xyz.ksharma.huezoo.platform.billing

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.huezoo.platform.AndroidActivityProvider
import kotlin.coroutines.resume
import com.android.billingclient.api.BillingClient as GoogleBillingClient

class AndroidBillingClient(
    private val activityProvider: AndroidActivityProvider,
    context: Context,
    private val onPurchaseSuccess: () -> Unit = {},
) : BillingClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val googleClient: GoogleBillingClient = GoogleBillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        // Billing Library 8+ manages transient disconnections itself; the library
        // reconnects automatically instead of relying on manual retry logic.
        .enableAutoServiceReconnection()
        .setListener(purchasesUpdatedListener())
        .build()

    init {
        // Eager connection so startup reconciliation runs without waiting for the user
        // to open the upgrade or settings screen. If the user already owns
        // [PRODUCT_UNLIMITED] — including via a purchase that was acknowledged but never
        // committed to DataStore due to a prior crash — onPurchaseSuccess re-grants it.
        connectAndReconcile()
    }

    private fun connectAndReconcile() {
        if (googleClient.isReady) {
            scope.launch { queryExistingPurchases() }
            return
        }
        googleClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == GoogleBillingClient.BillingResponseCode.OK) {
                    scope.launch { queryExistingPurchases() }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Will reconnect on next operation via ensureConnected()
            }
        })
    }

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(GoogleBillingClient.ProductType.INAPP)
            .build()
        val result = runCatching { googleClient.queryPurchasesAsync(params) }.getOrNull() ?: return
        if (result.billingResult.responseCode != GoogleBillingClient.BillingResponseCode.OK) return
        val owned = result.purchasesList.any { purchase ->
            purchase.products.contains(PRODUCT_UNLIMITED) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (owned) onPurchaseSuccess()
    }

    private fun purchasesUpdatedListener() = PurchasesUpdatedListener { result, purchases ->
        // Always grant entitlement for any PURCHASED purchase BEFORE touching the
        // pending deferred. Covers orphaned purchases delivered without a live
        // deferred (process death between acknowledge and DataStore commit, promo
        // redemptions, queued updates after reconnect).
        if (result.responseCode == GoogleBillingClient.BillingResponseCode.OK) {
            purchases?.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                    onPurchaseSuccess()
                }
            }
        }

        val deferred = pendingPurchase.also { pendingPurchase = null }
            ?: return@PurchasesUpdatedListener
        when {
            result.responseCode == GoogleBillingClient.BillingResponseCode.OK &&
                !purchases.isNullOrEmpty() ->
                deferred.complete(PurchaseResult.Success)
            result.responseCode == GoogleBillingClient.BillingResponseCode.USER_CANCELED ->
                deferred.complete(PurchaseResult.Cancelled)
            else ->
                deferred.complete(PurchaseResult.Error(result.debugMessage))
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        googleClient.acknowledgePurchase(params) { _ -> }
    }

    private suspend fun ensureConnected() {
        if (googleClient.isReady) return
        suspendCancellableCoroutine<Unit> { cont ->
            googleClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(Unit)
                }
                override fun onBillingServiceDisconnected() {
                    // Will reconnect on next call
                }
            })
        }
    }

    private suspend fun queryProductDetailsInternal(productId: String): ProductDetailsResult {
        ensureConnected()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(GoogleBillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        return googleClient.queryProductDetails(params)
    }

    override suspend fun queryPrice(productId: String): String? = runCatching {
        queryProductDetailsInternal(productId)
            .productDetailsList
            ?.firstOrNull()
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
    }.getOrNull()

    override suspend fun purchase(productId: String): PurchaseResult {
        val activity = activityProvider.get()
            ?: return PurchaseResult.Error("No active Activity")
        return runCatching {
            val details = queryProductDetailsInternal(productId)
                .productDetailsList
                ?.firstOrNull()
                ?: return PurchaseResult.Error("Product not found: $productId")

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build(),
                    ),
                )
                .build()

            val deferred = CompletableDeferred<PurchaseResult>()
            pendingPurchase = deferred
            googleClient.launchBillingFlow(activity, flowParams)
            deferred.await()
        }.getOrElse { PurchaseResult.Error(it.message ?: "Unknown error") }
    }

    override suspend fun isOwned(productId: String): Boolean = runCatching {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(GoogleBillingClient.ProductType.INAPP)
            .build()
        googleClient.queryPurchasesAsync(params)
            .purchasesList
            .any { it.products.contains(productId) }
    }.getOrDefault(false)
}
