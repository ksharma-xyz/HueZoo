package xyz.ksharma.huezoo.platform.billing

interface BillingClient {
    /** Returns the formatted price string for [productId], or null if unavailable. */
    suspend fun queryPrice(productId: String): String?

    /** Initiates a purchase flow for [productId]. */
    suspend fun purchase(productId: String): PurchaseResult

    /** Returns true if the player already owns [productId]. */
    suspend fun isOwned(productId: String): Boolean
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}

/** Product ID for the one-time Unlimited upgrade. */
const val PRODUCT_UNLIMITED = "xyz.ksharma.huezoo.unlimited"
