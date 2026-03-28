package xyz.ksharma.huezoo.platform.ads

/**
 * iOS stub for [RewardedAdClient].
 * AdMob iOS integration is deferred to a future phase.
 */
class IosRewardedAdClient : RewardedAdClient {
    override val isReady: Boolean = false
    override suspend fun load() = Unit
    override suspend fun show(): AdResult = AdResult.Error("AdMob iOS not yet implemented")
}
