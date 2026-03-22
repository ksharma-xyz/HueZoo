package xyz.ksharma.huezoo.data.repository.di

import org.koin.dsl.module
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.data.repository.impl.DefaultDailyRepository
import xyz.ksharma.huezoo.data.repository.impl.DefaultSettingsRepository
import xyz.ksharma.huezoo.data.repository.impl.DefaultThresholdRepository

val repositoryModule = module {
    single<ThresholdRepository> {
        DefaultThresholdRepository(
            db = get(),
            platformOps = get(),
            settingsRepository = get(),
        )
    }
    single<DailyRepository> { DefaultDailyRepository(db = get()) }
    single<SettingsRepository> { DefaultSettingsRepository(db = get()) }
}
