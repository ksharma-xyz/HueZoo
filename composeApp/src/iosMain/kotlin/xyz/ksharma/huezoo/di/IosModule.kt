package xyz.ksharma.huezoo.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.platform.IosPlatformOps
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.billing.BillingClient
import xyz.ksharma.huezoo.platform.billing.IosBillingClient
import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.IosHapticEngine

val iosModule = module {
    singleOf(::IosPlatformOps) { bind<PlatformOps>() }
    singleOf(::IosHapticEngine) { bind<HapticEngine>() }
    single<BillingClient> {
        val settings = get<SettingsRepository>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        IosBillingClient(
            onPurchaseSuccess = { scope.launch { settings.setPaid(true) } },
        )
    }
}
