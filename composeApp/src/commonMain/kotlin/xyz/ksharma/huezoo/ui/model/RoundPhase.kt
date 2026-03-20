package xyz.ksharma.huezoo.ui.model

/**
 * Shared round-phase state used by both The Threshold and the Daily Challenge.
 *
 * Drives [RadialSwatchLayout] animations and controls when taps are accepted.
 */
enum class RoundPhase {
    /** Waiting for the player to tap a swatch. */
    Idle,

    /** Player tapped the correct swatch — show green feedback. */
    Correct,

    /** Player tapped the wrong swatch — show shake + sting. */
    Wrong,

    /**
     * Flower is folding away before the next round's swatches are emitted.
     * Interaction is disabled; the current swatch state stays visible while
     * the petals retract to the centre point.
     */
    FoldingOut,
}
