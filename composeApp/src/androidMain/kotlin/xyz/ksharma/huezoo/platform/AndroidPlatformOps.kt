package xyz.ksharma.huezoo.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AndroidPlatformOps(private val context: Context) : PlatformOps {

    override val isDebugBuild: Boolean
        get() = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    override val appVersion: String
        get() = try {
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
                .versionName ?: "0.1"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.1"
        }

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
