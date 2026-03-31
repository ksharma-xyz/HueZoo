package xyz.ksharma.huezoo.testutil

import xyz.ksharma.huezoo.platform.billing.BillingClient
import xyz.ksharma.huezoo.platform.billing.PurchaseResult

/**
 * No-op [BillingClient] for unit tests.
 *
 * [queryPrice] returns null (no store connection in tests).
 * [purchase] always returns [PurchaseResult.Cancelled] — tests that exercise
 * purchase flows should override behaviour via a subclass or constructor param.
 * [isOwned] returns false by default; set [owned] to control the result.
 */
class FakeBillingClient(
    private val owned: Boolean = false,
    private val priceLabel: String? = null,
) : BillingClient {
    override suspend fun queryPrice(productId: String): String? = priceLabel
    override suspend fun purchase(productId: String): PurchaseResult = PurchaseResult.Cancelled
    override suspend fun isOwned(productId: String): Boolean = owned
}

