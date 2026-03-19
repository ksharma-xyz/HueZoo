package xyz.ksharma.huezoo.ui.games.daily.state

import xyz.ksharma.huezoo.ui.model.SwatchUiModel

sealed interface DailyUiState {

    data object Loading : DailyUiState

    /** Player already completed today's challenge. */
    data class AlreadyPlayed(val score: Float) : DailyUiState

    data class Playing(
        val swatches: List<SwatchUiModel>,
        val deltaE: Float,
        /** 1-based. */
        val round: Int,
        val totalRounds: Int,
        val roundPhase: DailyRoundPhase,
    ) : DailyUiState
}

enum class DailyRoundPhase { Idle, Correct, Wrong }
