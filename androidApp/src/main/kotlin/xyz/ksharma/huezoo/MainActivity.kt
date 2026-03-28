package xyz.ksharma.huezoo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import xyz.ksharma.huezoo.platform.AndroidActivityProvider

class MainActivity : ComponentActivity() {

    private val activityProvider: AndroidActivityProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        activityProvider.set(this)
    }

    override fun onPause() {
        super.onPause()
        activityProvider.clear()
    }
}
