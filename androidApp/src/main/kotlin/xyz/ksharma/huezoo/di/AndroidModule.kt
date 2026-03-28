package xyz.ksharma.huezoo.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.huezoo.platform.AndroidActivityProvider
import xyz.ksharma.huezoo.platform.AndroidPlatformOps
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.ads.AndroidRewardedAdClient
import xyz.ksharma.huezoo.platform.ads.RewardedAdClient
import xyz.ksharma.huezoo.platform.billing.AndroidBillingClient
import xyz.ksharma.huezoo.platform.billing.BillingClient
import xyz.ksharma.huezoo.platform.haptics.AndroidHapticEngine
import xyz.ksharma.huezoo.platform.haptics.HapticEngine

val androidModule = module {
    single<PlatformOps> { AndroidPlatformOps(androidContext()) }
    single<HapticEngine> { AndroidHapticEngine(androidContext()) }
    single { AndroidActivityProvider() }
    single<BillingClient> { AndroidBillingClient(get(), androidContext()) }
    single<RewardedAdClient> { AndroidRewardedAdClient(androidContext(), get()) }
}
