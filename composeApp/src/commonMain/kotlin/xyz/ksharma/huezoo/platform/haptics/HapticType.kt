package xyz.ksharma.huezoo.platform.haptics

/**
 * All haptic moments in the app.
 *
 * Each value maps to a distinct physical pattern on each platform — implementations
 * decide the exact waveform / generator; callers only express intent.
 *
 * Naming follows user-perceived meaning, not hardware vocabulary:
 *   - [CorrectTap] / [WrongTap] — direct game outcomes
 *   - [MilestoneHit] — ΔE tier crossed (layered 200 ms after CorrectTap)
 *   - [PerfectRun] — all rounds correct in Daily (two-beat celebration)
 *   - [GameOver] — all tries exhausted in Threshold
 *   - [Selection] / [ButtonTap] / [GemEarned] — reserved for UI-level feedback
 */
enum class HapticType {
    // ── UI confirmation ───────────────────────────────────────────────────────
    /** Lightest tap — generic "I received your input" for nav / selection. */
    Selection,

    /** Light click — primary button press confirmation. */
    ButtonTap,

    /** Soft tick — gem counter increment on return to Home. */
    GemEarned,

    // ── Game outcomes ─────────────────────────────────────────────────────────
    /** Crisp medium snap — the player found the odd swatch. */
    CorrectTap,

    /** Distinctive double-bump — the player tapped the wrong swatch. */
    WrongTap,

    // ── Escalating events ─────────────────────────────────────────────────────
    /** Heavy single thud — ΔE dropped below a milestone boundary (fired 200 ms after CorrectTap). */
    MilestoneHit,

    /** Two-beat celebration — Daily perfect run (all 6 rounds correct). */
    PerfectRun,

    /** Long heavy fade — all Threshold tries exhausted. */
    GameOver,
}
