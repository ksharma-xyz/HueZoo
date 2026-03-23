package xyz.ksharma.huezoo.platform.haptics

/**
 * Platform-agnostic haptic interface.
 *
 * Callers express *intent* via [HapticType]. Each platform implementation
 * translates that intent into the best available hardware pattern — callers
 * never need to know which generator or waveform is used.
 *
 * Implementations must be safe to call from the main thread.
 * All calls are fire-and-forget; no return value or error surface.
 */
interface HapticEngine {
    fun perform(type: HapticType)
}
