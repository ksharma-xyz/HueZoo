package xyz.ksharma.huezoo.platform

import android.content.Context
import android.content.Intent

class AndroidPlatformOps(private val context: Context) : PlatformOps {

    override fun shareText(text: String, title: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = Intent.createChooser(intent, title.ifBlank { null })
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
