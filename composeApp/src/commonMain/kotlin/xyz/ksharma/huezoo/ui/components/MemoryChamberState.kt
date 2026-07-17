package xyz.ksharma.huezoo.ui.components

/**
 * Visual state of a Twin Lock memory chamber (Color Memory Match — Variation B).
 */
enum class MemoryChamberState {
    /** Color visible, LED pulsing — the chamber the player should study. */
    Live,

    /** Shutter down over the color — memory locked away. */
    Sealed,

    /** Locked, striped, showing "?" — not yet activated. */
    Waiting,

    /** Shutter up again, color visible — the answer-key reveal. */
    Revealed,
}
