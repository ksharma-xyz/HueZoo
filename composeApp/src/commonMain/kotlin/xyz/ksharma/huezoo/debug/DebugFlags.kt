package xyz.ksharma.huezoo.debug

/**
 * In-memory debug toggles. All flags default to false and are never persisted.
 * Only mutated from the Settings debug panel in debug builds.
 */
object DebugFlags {
    var forceStreakCelebration: Boolean = false
    var hideAds: Boolean = false

    /**
     * Gates the in-development Color Memory Match game (Game 6). Defaults to false so
     * production users never see it; toggle on from the Settings debug panel to try it.
     * In-memory only — resets to false on every process start (and is unreachable in
     * release builds, where the debug panel is hidden).
     */
    var colorMemoryEnabled: Boolean = false
}
