package xyz.ksharma.huezoo.platform

interface PlatformOps {
    /** True when running a debug / development build. */
    val isDebugBuild: Boolean

    /** App version string from the platform manifest (e.g. "0.1.0" on Android, "0.1" on iOS). */
    val appVersion: String

    /** Share plain text via the native system share sheet. */
    fun shareText(text: String, title: String = "")
}
