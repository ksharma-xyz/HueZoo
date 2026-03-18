package xyz.ksharma.huezoo.platform

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

class IosPlatformOps : PlatformOps {

    @OptIn(ExperimentalForeignApi::class)
    override fun shareText(text: String, title: String) {
        val activityViewController = UIActivityViewController(listOf(text), null)

        activityViewController.popoverPresentationController?.apply {
            val application = UIApplication.sharedApplication
            sourceView = application.keyWindow

            val window = application.keyWindow
            sourceRect = if (window != null) {
                val frame: CValue<CGRect> = window.frame
                cValue {
                    this.origin.x = frame.useContents { this.origin.x }
                    this.origin.y = frame.useContents { this.origin.y }
                    this.size.width = frame.useContents { this.size.width }
                    this.size.height = frame.useContents { this.size.height }
                }
            } else {
                cValue { }
            }
            permittedArrowDirections = 0u
        }

        NSOperationQueue.mainQueue.addOperationWithBlock {
            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.presentViewController(
                    activityViewController,
                    animated = true,
                    completion = null,
                )
        }
    }
}
