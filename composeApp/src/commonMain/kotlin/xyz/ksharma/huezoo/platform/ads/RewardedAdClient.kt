package xyz.ksharma.huezoo.platform.ads

interface RewardedAdClient {
    /** True when an ad is loaded and ready to display. */
    val isReady: Boolean

    /** Loads the next rewarded ad in the background. Call after each show. */
    suspend fun load()

    /**
     * Shows the loaded rewarded ad.
     * Returns [AdResult.Rewarded] if the user watched enough to earn the reward,
     * [AdResult.Dismissed] if they closed early, or [AdResult.Error] on failure.
     */
    suspend fun show(): AdResult
}

sealed interface AdResult {
    data object Rewarded : AdResult
    data object Dismissed : AdResult
    data class Error(val message: String) : AdResult
}
