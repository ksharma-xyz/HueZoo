package xyz.ksharma.huezoo.ui.copy

import kotlin.random.Random

/**
 * Canonical sting copy pools for Color Memory Match — from the design handoff
 * (`docs/new_game/design_handoff_color_memory_match`). Do not paraphrase.
 */
object CMMatchStingCopy {

    private val correctEasy = listOf("Locked in.", "Eyes still warm.", "Steady.")
    private val correctMid = listOf("Sharp.", "Eye remembered.", "Held the line.")
    private val correctHard = listOf("Real recall.", "That one was real.", "Eyes did not blink.")
    private val correctElite = listOf("Beyond impressive.", "Your retina is a recorder.", "Eyes elite.")

    private val wrongEasy = listOf("That gap was wide. Hmm.", "Warmer than that.", "The eye wandered.")
    private val wrongMid = listOf("Close, but ΔE {de} got you.", "Memory blinked at ΔE {de}.", "Almost. ΔE {de}.")
    private val wrongHard = listOf(
        "ΔE {de}. Sub-pixel territory.",
        "You were right there. ΔE {de}.",
        "Memory faded at ΔE {de}.",
    )
    private val wrongElite = listOf(
        "ΔE {de}. Barely real. Still missed.",
        "Below human limits. So is the gap.",
        "You almost out-saw your own eye.",
    )

    private const val TIER_EASY = 3.5f
    private const val TIER_MID = 2.0f
    private const val TIER_HARD = 1.0f
    private const val DECIMALS_1 = 10
    private const val DECIMALS_2 = 100

    /**
     * Picks a sting line for the just-answered round.
     *
     * @param deltaE the round's ΔE (curve value — difficulty tier selector).
     * @param random pass a seeded [Random] for deterministic tests.
     */
    fun forRound(correct: Boolean, deltaE: Float, random: Random = Random.Default): String {
        val pool = when {
            deltaE >= TIER_EASY -> if (correct) correctEasy else wrongEasy
            deltaE >= TIER_MID -> if (correct) correctMid else wrongMid
            deltaE >= TIER_HARD -> if (correct) correctHard else wrongHard
            else -> if (correct) correctElite else wrongElite
        }
        return pool.random(random).replace("{de}", deltaE.formatDeltaE())
    }

    /** Result-screen sting by total session score. */
    @Suppress("MagicNumber")
    fun resultStingByScore(score: Int): String = when {
        score >= 95 -> "Superhuman recall. Seriously."
        score >= 80 -> "Your memory is elite."
        score >= 60 -> "Sharp. Very sharp."
        score >= 40 -> "Better than most."
        score >= 20 -> "Room to grow."
        score > 0 -> "Keep training."
        else -> "The eye drifted today. Come back."
    }

    /** Result-card tier label by total session score. */
    @Suppress("MagicNumber")
    fun tierLabelByScore(score: Int): String = when {
        score >= 95 -> "PERFECT EYE"
        score >= 80 -> "STRONG RECALL"
        score >= 60 -> "SOLID"
        score >= 40 -> "ROOM TO GROW"
        score >= 10 -> "EYES DRIFTED"
        else -> "FLATLINED"
    }

    /** 1 decimal for ΔE ≥ 1.0, 2 decimals below. */
    private fun Float.formatDeltaE(): String {
        val scale = if (this >= 1.0f) DECIMALS_1 else DECIMALS_2
        val scaled = (this * scale).toInt()
        val intPart = scaled / scale
        val decPart = scaled % scale
        return if (scale == DECIMALS_1) {
            "$intPart.$decPart"
        } else {
            "$intPart.${decPart.toString().padStart(2, '0')}"
        }
    }
}
