package xyz.ksharma.huezoo.testutil

import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.HapticType

/** No-op [HapticEngine] for unit tests. */
class FakeHapticEngine : HapticEngine {
    override fun perform(type: HapticType) = Unit
}
