package xyz.ksharma.huezoo.ui.games.threshold.state

import kotlinx.datetime.Instant
import xyz.ksharma.huezoo.ui.model.SwatchUiModel

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
    ) : ThresholdUiState
}

enum class RoundPhase { Idle, Correct, Wrong }
