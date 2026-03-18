package xyz.ksharma.huezoo.di

import org.koin.dsl.module
import xyz.ksharma.huezoo.data.db.di.databaseModule

val appModule = module {
    includes(databaseModule)
}
