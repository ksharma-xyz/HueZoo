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

    // Score tier cut-offs, as a percentage of the achievable max score.
    private const val PERCENT_SCALE = 100
    private const val PCT_PERFECT = 95
    private const val PCT_STRONG = 80
    private const val PCT_SOLID = 60
    private const val PCT_ROOM = 40
    private const val PCT_DRIFTED = 20
    private const val PCT_MIN = 10

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

    /** Result-screen sting by score, tiered on the fraction of [maxScore] achieved. */
    fun resultStingByScore(score: Int, maxScore: Int): String {
        val pct = percent(score, maxScore)
        return when {
            pct >= PCT_PERFECT -> "Superhuman recall. Seriously."
            pct >= PCT_STRONG -> "Your memory is elite."
            pct >= PCT_SOLID -> "Sharp. Very sharp."
            pct >= PCT_ROOM -> "Better than most."
            pct >= PCT_DRIFTED -> "Room to grow."
            score > 0 -> "Keep training."
            else -> "The eye drifted today. Come back."
        }
    }

    /** Result-card tier label by score, tiered on the fraction of [maxScore] achieved. */
    fun tierLabelByScore(score: Int, maxScore: Int): String {
        val pct = percent(score, maxScore)
        return when {
            pct >= PCT_PERFECT -> "PERFECT EYE"
            pct >= PCT_STRONG -> "STRONG RECALL"
            pct >= PCT_SOLID -> "SOLID"
            pct >= PCT_ROOM -> "ROOM TO GROW"
            pct >= PCT_MIN -> "EYES DRIFTED"
            else -> "FLATLINED"
        }
    }

    /** Score as a percentage of the achievable max (0 when [maxScore] ≤ 0). */
    private fun percent(score: Int, maxScore: Int): Int =
        if (maxScore <= 0) 0 else (score * PERCENT_SCALE) / maxScore

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
