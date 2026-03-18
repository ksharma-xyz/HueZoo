package xyz.ksharma.huezoo.data.db.di

import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.ksharma.huezoo.data.db.DatabaseDriverFactory
import xyz.ksharma.huezoo.data.db.HuezooDatabase

val databaseModule = module {
    includes(platformDatabaseModule)
    single { HuezooDatabase(get<DatabaseDriverFactory>().createDriver()) }
}

expect val platformDatabaseModule: Module
