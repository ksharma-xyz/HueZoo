package xyz.ksharma.huezoo.di

import org.koin.dsl.module
import xyz.ksharma.huezoo.data.db.di.databaseModule
import xyz.ksharma.huezoo.data.repository.di.repositoryModule
import xyz.ksharma.huezoo.domain.color.di.colorModule
import xyz.ksharma.huezoo.domain.game.SessionResultCache
import xyz.ksharma.huezoo.domain.game.di.gameEngineModule
import xyz.ksharma.huezoo.platform.ads.AdOrchestrator
import xyz.ksharma.huezoo.ui.di.viewModelModule
import xyz.ksharma.huezoo.ui.model.PlayerState

val appModule = module {
    includes(
        databaseModule,
        colorModule,
        gameEngineModule,
        repositoryModule,
        viewModelModule,
    )
    single { PlayerState() }
    single { SessionResultCache() }
    single { AdOrchestrator() }
}
