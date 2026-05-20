package xyz.ksharma.huezoo.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.platform.AndroidActivityProvider
import xyz.ksharma.huezoo.platform.AndroidPlatformOps
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.billing.AndroidBillingClient
import xyz.ksharma.huezoo.platform.billing.BillingClient
import xyz.ksharma.huezoo.platform.haptics.AndroidHapticEngine
import xyz.ksharma.huezoo.platform.haptics.HapticEngine

val androidModule = module {
    single<PlatformOps> { AndroidPlatformOps(androidContext()) }
    single<HapticEngine> { AndroidHapticEngine(androidContext()) }
    single { AndroidActivityProvider() }
    single<BillingClient> {
        val settings = get<SettingsRepository>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        AndroidBillingClient(
            activityProvider = get(),
            context = androidContext(),
            onPurchaseSuccess = { scope.launch { settings.setPaid(true) } },
        )
    }
}
