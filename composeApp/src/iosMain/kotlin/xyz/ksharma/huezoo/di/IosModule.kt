package xyz.ksharma.huezoo.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.platform.IosPlatformOps
import xyz.ksharma.huezoo.platform.PlatformOps

val iosModule = module {
    singleOf(::IosPlatformOps) { bind<PlatformOps>() }
}
