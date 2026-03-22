package xyz.ksharma.huezoo.ui.games.daily.state

import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
import xyz.ksharma.huezoo.ui.model.SwatchUiModel

sealed interface DailyUiState {

    data object Loading : DailyUiState

    /** Player already completed today's challenge. */
    data object AlreadyPlayed : DailyUiState

    data class Playing(
        val swatches: List<SwatchUiModel>,
        val deltaE: Float,
        /** 1-based. */
        val round: Int,
        val totalRounds: Int,
        val roundPhase: RoundPhase,
        /** Shape style for this round — changes each round like Threshold. */
        val layoutStyle: SwatchLayoutStyle = SwatchLayoutStyle.Flower,
        /**
         * Monotonically increasing — increments on every [emitRound] call so
         * [RadialSwatchLayout] always triggers its unfold animation.
         */
        val roundGeneration: Int = 0,
    ) : DailyUiState
}
