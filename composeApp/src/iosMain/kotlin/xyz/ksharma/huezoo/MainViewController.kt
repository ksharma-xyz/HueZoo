package xyz.ksharma.huezoo

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.context.startKoin
import xyz.ksharma.huezoo.di.appModule
import xyz.ksharma.huezoo.di.iosModule

fun MainViewController() = ComposeUIViewController {
    App()
}

fun initKoin() {
    startKoin {
        modules(appModule, iosModule)
    }
}
