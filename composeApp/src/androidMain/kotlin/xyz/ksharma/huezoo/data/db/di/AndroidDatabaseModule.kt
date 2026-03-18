package xyz.ksharma.huezoo.data.db.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.data.db.AndroidDatabaseDriverFactory
import xyz.ksharma.huezoo.data.db.DatabaseDriverFactory

actual val platformDatabaseModule: Module = module {
    singleOf(::AndroidDatabaseDriverFactory) { bind<DatabaseDriverFactory>() }
}
