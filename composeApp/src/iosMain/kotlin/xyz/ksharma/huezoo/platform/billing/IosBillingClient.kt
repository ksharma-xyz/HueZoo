package xyz.ksharma.huezoo.platform.billing

/**
 * iOS stub for [BillingClient].
 * StoreKit 2 integration is deferred to a future phase.
 */
class IosBillingClient : BillingClient {
    override suspend fun queryPrice(productId: String): String? = null
    override suspend fun purchase(productId: String): PurchaseResult =
        PurchaseResult.Error("StoreKit 2 not yet implemented")
    override suspend fun isOwned(productId: String): Boolean = false
}
