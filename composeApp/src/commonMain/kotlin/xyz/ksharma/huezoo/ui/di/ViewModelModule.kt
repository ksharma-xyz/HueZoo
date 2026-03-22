package xyz.ksharma.huezoo.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.games.daily.DailyViewModel
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdViewModel
import xyz.ksharma.huezoo.ui.home.HomeViewModel
import xyz.ksharma.huezoo.ui.leaderboard.LeaderboardViewModel
import xyz.ksharma.huezoo.ui.result.ResultViewModel
import xyz.ksharma.huezoo.ui.settings.SettingsViewModel

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ThresholdViewModel)
    viewModelOf(::DailyViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModel { params ->
        ResultViewModel(
            navResult = params.get<Result>(),
            thresholdRepository = get(),
            dailyRepository = get(),
        )
    }
}
