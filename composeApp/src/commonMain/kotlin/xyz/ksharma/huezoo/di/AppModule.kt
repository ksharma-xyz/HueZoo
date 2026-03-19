package xyz.ksharma.huezoo.di

import org.koin.dsl.module
import xyz.ksharma.huezoo.data.db.di.databaseModule
import xyz.ksharma.huezoo.domain.color.di.colorModule

val appModule = module {
    includes(
        databaseModule,
        colorModule,
    )
}
