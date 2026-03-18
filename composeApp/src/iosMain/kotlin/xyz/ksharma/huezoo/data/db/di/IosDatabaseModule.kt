package xyz.ksharma.huezoo.data.db.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.data.db.DatabaseDriverFactory
import xyz.ksharma.huezoo.data.db.IosDatabaseDriverFactory

actual val platformDatabaseModule: Module = module {
    singleOf(::IosDatabaseDriverFactory) { bind<DatabaseDriverFactory>() }
}
