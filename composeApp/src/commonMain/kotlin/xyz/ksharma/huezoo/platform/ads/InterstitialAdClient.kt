package xyz.ksharma.huezoo.platform.ads

/**
 * Platform-agnostic interstitial ad client.
 * Implementations: [AndroidInterstitialAdClient] (real), [IosInterstitialAdClient] (stub).
 *
 * Usage:
 *   1. Call [load] in the background before the ad is needed.
 *   2. Call [show] to display the ad (suspends until dismissed or error).
 *      Returns [AdResult.Dismissed] on normal close; [AdResult.Error] on failure/no-load.
 */
interface InterstitialAdClient {
    suspend fun load()
    suspend fun show(): AdResult
}
