package xyz.ksharma.huezoo.domain.game

import xyz.ksharma.huezoo.domain.color.ColorPair

/**
 * Generates rounds for Color Memory Match (Game 6).
 *
 * Each round shows Color A for a few seconds, seals it, then reveals Color B.
 * The player calls SAME or DIFFERENT. ΔE tightens each round per [deltaECurve].
 * Stateless — callers track which round they are on.
 */
interface ColorMemoryGameEngine {

    /** Fixed number of rounds per session. */
    val totalRounds: Int

    /** Fixed ΔE per round — hardest last. Index corresponds to 0-based round index. */
    val deltaECurve: List<Float>

    /**
     * Generates the color pair for [round] (1-based). Whether the pair is same or
     * different is a 50/50 coin flip per round.
     */
    fun generateRound(round: Int): ColorPair

    companion object {
        /** Points awarded per correct answer. */
        const val POINTS_CORRECT = 10

        /** Points deducted per wrong answer — score may go negative. */
        const val POINTS_WRONG = -5

        /** Maximum achievable session score (10 rounds × +10). */
        const val MAX_SCORE = 100
    }
}
