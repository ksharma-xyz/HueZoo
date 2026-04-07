package xyz.ksharma.huezoo.platform.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/** Call once on Application#onCreate to initialize the Mobile Ads SDK. */
fun initMobileAds(context: Context) {
    MobileAds.setRequestConfiguration(
        RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
    )
    MobileAds.initialize(context)
}
