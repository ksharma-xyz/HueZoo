package xyz.ksharma.funwithcolors

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import xyz.ksharma.funwithcolors.di.androidModule
import xyz.ksharma.funwithcolors.di.appModule

class FunWithColorsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FunWithColorsApp)
            modules(appModule, androidModule)
        }
    }
}
