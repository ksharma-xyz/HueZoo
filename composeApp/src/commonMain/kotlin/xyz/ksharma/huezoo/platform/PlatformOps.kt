package xyz.ksharma.huezoo.platform

interface PlatformOps {
    /** Share plain text via the native system share sheet. */
    fun shareText(text: String, title: String = "")
}
