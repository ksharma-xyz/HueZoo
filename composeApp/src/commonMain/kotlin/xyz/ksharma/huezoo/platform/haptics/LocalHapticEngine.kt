package xyz.ksharma.huezoo.platform.haptics

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal that provides the active [HapticEngine] to any composable in the tree.
 *
 * Set once in [App] via [androidx.compose.runtime.CompositionLocalProvider].
 * The default is [NoOpHapticEngine] so Compose Previews and tests never crash.
 *
 * Usage in composables:
 * ```
 * val haptic = LocalHapticEngine.current
 * haptic.perform(HapticType.ButtonTap)
 * ```
 */
val LocalHapticEngine = compositionLocalOf<HapticEngine> { NoOpHapticEngine }

/**
 * Silent no-op implementation used as the CompositionLocal default.
 * Ensures previews and unit tests work without a real platform engine in scope.
 */
private object NoOpHapticEngine : HapticEngine {
    override fun perform(type: HapticType) = Unit
}
