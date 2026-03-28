package xyz.ksharma.huezoo.platform.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds

/** Call once on Application#onCreate to initialize the Mobile Ads SDK. */
fun initMobileAds(context: Context) {
    MobileAds.initialize(context)
}
