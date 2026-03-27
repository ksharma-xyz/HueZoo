package xyz.ksharma.huezoo.testutil

import xyz.ksharma.huezoo.platform.PlatformOps

/**
 * No-op [PlatformOps] for unit tests.
 *
 * Only [isDebugBuild] is read by repositories (to compute [maxAttempts]).
 * [shareText] is never called from repository or engine code.
 */
class FakePlatformOps(override val isDebugBuild: Boolean = false) : PlatformOps {
    override fun shareText(text: String, title: String) = Unit
}
