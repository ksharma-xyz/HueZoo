package xyz.ksharma.huezoo.domain.game.di

import org.koin.dsl.module
import xyz.ksharma.huezoo.domain.game.DailyGameEngine
import xyz.ksharma.huezoo.domain.game.DefaultDailyGameEngine
import xyz.ksharma.huezoo.domain.game.DefaultThresholdGameEngine
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine

val gameEngineModule = module {
    single<ThresholdGameEngine> { DefaultThresholdGameEngine(colorEngine = get()) }
    single<DailyGameEngine> { DefaultDailyGameEngine(colorEngine = get()) }
}
