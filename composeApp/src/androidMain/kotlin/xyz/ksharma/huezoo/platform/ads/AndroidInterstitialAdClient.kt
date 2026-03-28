package xyz.ksharma.huezoo.platform.ads

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.huezoo.platform.AndroidActivityProvider
import kotlin.coroutines.resume

/**
 * Android implementation of [InterstitialAdClient] using Google Mobile Ads SDK.
 *
 * Requires `MobileAds.initialize(context)` called once on app startup.
 * Uses the test ad unit ID — replace before shipping.
 */
class AndroidInterstitialAdClient(
    private val context: Context,
    private val activityProvider: AndroidActivityProvider,
) : InterstitialAdClient {

    private var loadedAd: InterstitialAd? = null

    override suspend fun load() {
        loadedAd = suspendCancellableCoroutine { cont ->
            InterstitialAd.load(
                context,
                AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) = cont.resume(ad)
                    override fun onAdFailedToLoad(error: LoadAdError) = cont.resume(null)
                },
            )
        }
    }

    override suspend fun show(): AdResult {
        val ad = loadedAd ?: return AdResult.Error("No interstitial ad loaded")
        val activity = activityProvider.get() ?: return AdResult.Error("No active Activity")

        val result = CompletableDeferred<AdResult>()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadedAd = null
                if (!result.isCompleted) result.complete(AdResult.Dismissed)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadedAd = null
                result.complete(AdResult.Error(error.message))
            }
        }

        ad.show(activity)
        return result.await()
    }

    private companion object {
        // Test interstitial ad unit ID — replace before shipping.
        const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }
}
