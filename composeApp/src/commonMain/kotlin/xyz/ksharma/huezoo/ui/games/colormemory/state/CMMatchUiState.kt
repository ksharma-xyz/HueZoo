package xyz.ksharma.huezoo.ui.games.colormemory.state

import androidx.compose.ui.graphics.Color

/**
 * Phase state machine for a Color Memory Match round:
 *
 * ```
 * [Memory]   --3000ms--> [Hold]
 * [Hold]     --400ms-->  [Recall]
 * [Recall]   --player tap--> [Feedback]
 * [Feedback] --1700ms--> next round (round += 1) or session end
 * ```
 */
enum class CMMatchPhase {
    /** Color A visible in Chamber A, Chamber B waiting. */
    Memory,

    /** Chamber A sealed behind the shutter — brief interstitial. */
    Hold,

    /** Color B visible in Chamber B; SAME / DIFFERENT enabled. */
    Recall,

    /** Chamber A unsealed for the answer-key reveal + sting copy. */
    Feedback,
}

/** Outcome of a completed round. */
enum class CMMRoundResult { Correct, Wrong }

/**
 * Feedback payload for the just-answered round.
 *
 * @param sting Pre-picked sting line — chosen once in the ViewModel so it stays
 *   stable across recompositions.
 */
data class CMMLastAnswer(
    val correct: Boolean,
    val truthSame: Boolean,
    val deltaE: Float,
    val sting: String,
)

sealed interface CMMatchUiState {

    data object Loading : CMMatchUiState

    data class Playing(
        /** 1-based. */
        val round: Int,
        val totalRounds: Int,
        val phase: CMMatchPhase,
        /** Running score — may go negative. */
        val score: Int,
        /** Completed-round outcomes, size = round − 1 (or `round` during Feedback). */
        val roundResults: List<CMMRoundResult>,
        val colorA: Color,
        val colorB: Color,
        /** Target ΔE of the current round (0 when the pair is identical). */
        val currentDeltaE: Float,
        /** ΔE the current round is played at — always the curve value, even for same-pairs. */
        val roundDeltaE: Float,
        val lastAnswer: CMMLastAnswer? = null,
        /** Increments every round so entrance animations always re-trigger. */
        val roundGeneration: Int = 0,
    ) : CMMatchUiState
}
