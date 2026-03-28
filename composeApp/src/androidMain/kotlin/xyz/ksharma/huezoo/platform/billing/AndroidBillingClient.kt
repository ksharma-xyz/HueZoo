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
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.huezoo.platform.AndroidActivityProvider
import kotlin.coroutines.resume
import com.android.billingclient.api.BillingClient as GoogleBillingClient

class AndroidBillingClient(
    private val activityProvider: AndroidActivityProvider,
    context: Context,
) : BillingClient {

    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val googleClient: GoogleBillingClient = GoogleBillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .setListener(purchasesUpdatedListener())
        .build()

    private fun purchasesUpdatedListener() = PurchasesUpdatedListener { result, purchases ->
        val deferred = pendingPurchase ?: return@PurchasesUpdatedListener
        pendingPurchase = null
        when {
            result.responseCode == GoogleBillingClient.BillingResponseCode.OK &&
                !purchases.isNullOrEmpty() -> {
                acknowledgePurchase(purchases.first())
                deferred.complete(PurchaseResult.Success)
            }
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
                    cont.resume(Unit)
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
