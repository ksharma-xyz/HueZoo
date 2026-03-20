package xyz.ksharma.huezoo.ui.games.threshold.state

import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
sealed interface ThresholdUiState {

    data object Loading : ThresholdUiState

    /** All attempts in the current 8-hour window are used. */
    data class Blocked(
        val nextResetAt: Instant,
        val attemptsUsed: Int,
        val maxAttempts: Int,
    ) : ThresholdUiState

    data class Playing(
        val swatches: List<SwatchUiModel>,
        val deltaE: Float,
        /** 1-based round counter — increments on each correct tap. */
        val round: Int,
        val attemptsRemaining: Int,
        val roundPhase: RoundPhase,
        /** Ego-sting shown during Wrong phase; null otherwise. */
        val stingCopy: String? = null,
        val totalGems: Int = 0,
    ) : ThresholdUiState
}

enum class RoundPhase { Idle, Correct, Wrong }
