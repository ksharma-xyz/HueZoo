package xyz.ksharma.huezoo.ui.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.ui.eyestrain.EyeStrainViewModel
import xyz.ksharma.huezoo.ui.games.daily.DailyViewModel
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdViewModel
import xyz.ksharma.huezoo.ui.home.HomeViewModel
import xyz.ksharma.huezoo.ui.leaderboard.LeaderboardViewModel
import xyz.ksharma.huezoo.ui.paywall.PaywallViewModel
import xyz.ksharma.huezoo.ui.result.ResultViewModel
import xyz.ksharma.huezoo.ui.settings.SettingsViewModel
import xyz.ksharma.huezoo.ui.splash.SplashViewModel
import xyz.ksharma.huezoo.ui.upgrade.UpgradeViewModel

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::EyeStrainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ThresholdViewModel)
    viewModelOf(::DailyViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModelOf(::ResultViewModel)
    viewModelOf(::UpgradeViewModel)
    viewModelOf(::PaywallViewModel)
}
