package xyz.ksharma.huezoo.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.platform.AndroidPlatformOps
import xyz.ksharma.huezoo.platform.PlatformOps

val androidModule = module {
    single<PlatformOps> { AndroidPlatformOps(androidContext()) }
}
