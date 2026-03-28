package xyz.ksharma.huezoo.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.platform.IosPlatformOps
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.ads.InterstitialAdClient
import xyz.ksharma.huezoo.platform.ads.IosInterstitialAdClient
import xyz.ksharma.huezoo.platform.ads.IosRewardedAdClient
import xyz.ksharma.huezoo.platform.ads.RewardedAdClient
import xyz.ksharma.huezoo.platform.billing.BillingClient
import xyz.ksharma.huezoo.platform.billing.IosBillingClient
import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.IosHapticEngine

val iosModule = module {
    singleOf(::IosPlatformOps) { bind<PlatformOps>() }
    singleOf(::IosHapticEngine) { bind<HapticEngine>() }
    singleOf(::IosBillingClient) { bind<BillingClient>() }
    singleOf(::IosRewardedAdClient) { bind<RewardedAdClient>() }
    singleOf(::IosInterstitialAdClient) { bind<InterstitialAdClient>() }
}
