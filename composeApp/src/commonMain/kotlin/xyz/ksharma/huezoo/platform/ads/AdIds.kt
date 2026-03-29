package xyz.ksharma.huezoo.platform.ads

import app.lexilabs.basic.ads.AdUnitId

/**
 * Central ad unit ID registry. Returns Google's official test IDs in debug builds
 * so no real inventory is consumed and no policy violations occur during development.
 *
 * Call [init] once from App() before any ad composable is composed.
 *
 * Test IDs: https://developers.google.com/admob/android/test-ads
 */
object AdIds {

    private var isDebug = false

    fun init(isDebugBuild: Boolean) {
        isDebug = isDebugBuild
    }

    val banner: String
        get() = AdUnitId.autoSelect(
            androidAdUnitId = if (isDebug) "ca-app-pub-3940256099942544/6300978111"
                              else "ca-app-pub-1771675816656791/1541300697",
            iosAdUnitId     = if (isDebug) "ca-app-pub-3940256099942544/2934735716"
                              else "ca-app-pub-1771675816656791/5831899491",
        )

    val interstitial: String
        get() = AdUnitId.autoSelect(
            androidAdUnitId = if (isDebug) "ca-app-pub-3940256099942544/1033173712"
                              else "ca-app-pub-1771675816656791/1736945079",
            iosAdUnitId     = if (isDebug) "ca-app-pub-3940256099942544/4411468910"
                              else "ca-app-pub-1771675816656791/4052117223",
        )

    val rewarded: String
        get() = AdUnitId.autoSelect(
            androidAdUnitId = if (isDebug) "ca-app-pub-3940256099942544/5224354917"
                              else "ca-app-pub-1771675816656791/5835972183",
            iosAdUnitId     = if (isDebug) "ca-app-pub-3940256099942544/1712485313"
                              else "ca-app-pub-1771675816656791/3345336954",
        )
}
