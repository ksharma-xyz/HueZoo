package xyz.ksharma.huezoo.domain.game

/**
 * All gem earn rates in one place — single source of truth for both game engines and tests.
 *
 * Threshold:
 *   - [THRESHOLD_CORRECT_TAP]     : base per correct tap
 *   - [THRESHOLD_MILESTONE_*]     : one-time bonus per try when ΔE first crosses a boundary
 *
 * Daily:
 *   - [DAILY_CORRECT_ROUND]       : per round answered correctly
 *   - [DAILY_PARTICIPATION]       : flat bonus just for completing all 6 rounds
 *   - [DAILY_PERFECT_BONUS]       : extra bonus for getting all 6 rounds correct
 */
object GameRewardRates {

    // ── Threshold ─────────────────────────────────────────────────────────────
    const val THRESHOLD_CORRECT_TAP = 2

    /** Awarded once per try the first time ΔE drops below each boundary. */
    const val THRESHOLD_MILESTONE_SHARP = 5 // ΔE < 2.0
    const val THRESHOLD_MILESTONE_EXPERT = 10 // ΔE < 1.0
    const val THRESHOLD_MILESTONE_ELITE = 25 // ΔE < 0.5

    /** ΔE boundaries for milestone bonuses (descending — evaluated in order). */
    val THRESHOLD_MILESTONES: List<Pair<Float, Int>> = listOf(
        0.5f to THRESHOLD_MILESTONE_ELITE,
        1.0f to THRESHOLD_MILESTONE_EXPERT,
        2.0f to THRESHOLD_MILESTONE_SHARP,
    )

    // ── Daily ─────────────────────────────────────────────────────────────────
    const val DAILY_CORRECT_ROUND = 5
    const val DAILY_PARTICIPATION = 3
    const val DAILY_PERFECT_BONUS = 20
}
