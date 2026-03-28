package xyz.ksharma.huezoo.platform.ads

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.huezoo.platform.AndroidActivityProvider
import kotlin.coroutines.resume

/**
 * Android implementation of [RewardedAdClient] using Google Mobile Ads SDK.
 *
 * Uses the test ad unit ID. Replace with the real unit ID before shipping.
 * Requires `MobileAds.initialize(context)` called once on app startup (in [HuezooApp]).
 */
class AndroidRewardedAdClient(
    private val context: Context,
    private val activityProvider: AndroidActivityProvider,
) : RewardedAdClient {

    private var loadedAd: RewardedAd? = null

    override val isReady: Boolean get() = loadedAd != null

    override suspend fun load() {
        loadedAd = suspendCancellableCoroutine { cont ->
            RewardedAd.load(
                context,
                AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) = cont.resume(ad)
                    override fun onAdFailedToLoad(error: LoadAdError) = cont.resume(null)
                },
            )
        }
    }

    override suspend fun show(): AdResult {
        val ad = loadedAd ?: return AdResult.Error("No ad loaded")
        val activity = activityProvider.get() ?: return AdResult.Error("No active Activity")

        val result = CompletableDeferred<AdResult>()
        var rewarded = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadedAd = null
                if (!result.isCompleted) {
                    result.complete(if (rewarded) AdResult.Rewarded else AdResult.Dismissed)
                }
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadedAd = null
                result.complete(AdResult.Error(error.message))
            }
        }

        ad.show(activity) { _ ->
            rewarded = true
        }

        return result.await()
    }

    private companion object {
        // Test rewarded ad unit ID — replace before shipping.
        const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
