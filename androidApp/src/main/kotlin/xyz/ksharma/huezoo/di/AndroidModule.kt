package xyz.ksharma.huezoo.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.huezoo.platform.AndroidPlatformOps
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.haptics.AndroidHapticEngine
import xyz.ksharma.huezoo.platform.haptics.HapticEngine

val androidModule = module {
    single<PlatformOps> { AndroidPlatformOps(androidContext()) }
    single<HapticEngine> { AndroidHapticEngine(androidContext()) }
}
