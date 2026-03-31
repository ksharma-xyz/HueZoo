package xyz.ksharma.huezoo.debug

/**
 * In-memory debug toggles. All flags default to false and are never persisted.
 * Only mutated from the Settings debug panel in debug builds.
 */
object DebugFlags {
    var forceStreakCelebration: Boolean = false
    var hideAds: Boolean = false
}
