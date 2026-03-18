package xyz.ksharma.huezoo

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import xyz.ksharma.huezoo.di.androidModule
import xyz.ksharma.huezoo.di.appModule

class FunWithColorsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FunWithColorsApp)
            modules(appModule, androidModule)
        }
    }
}
