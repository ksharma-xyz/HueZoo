package xyz.ksharma.huezoo.platform

interface PlatformOps {
    /** True when running a debug / development build. */
    val isDebugBuild: Boolean

    /** Share plain text via the native system share sheet. */
    fun shareText(text: String, title: String = "")
}
