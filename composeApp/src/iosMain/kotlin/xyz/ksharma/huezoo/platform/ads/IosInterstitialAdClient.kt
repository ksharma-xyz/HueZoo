package xyz.ksharma.huezoo.platform.ads

class IosInterstitialAdClient : InterstitialAdClient {
    override suspend fun load() = Unit
    override suspend fun show(): AdResult = AdResult.Error("Interstitial ads not yet implemented on iOS")
}
