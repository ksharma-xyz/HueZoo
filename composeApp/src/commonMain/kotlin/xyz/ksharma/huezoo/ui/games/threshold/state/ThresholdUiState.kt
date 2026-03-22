package xyz.ksharma.huezoo.ui.games.threshold.state

import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
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
        /** 1-based correct-tap counter within the current try. Shown in HUD as "TAP X". */
        val tap: Int,
        val attemptsRemaining: Int,
        val roundPhase: RoundPhase,
        /** Ego-sting shown during Wrong phase; null otherwise. */
        val stingCopy: String? = null,
        val totalGems: Int = 0,
        /** Shape style chosen randomly for this round. Changes every round. */
        val layoutStyle: SwatchLayoutStyle = SwatchLayoutStyle.Flower,
        /**
         * Monotonically increasing counter — increments on **every** [emitRound] call
         * (correct tap AND wrong-tap-with-lives-remaining).
         *
         * Used as the `roundKey` in [RadialSwatchLayout] so the unfold animation is
         * always triggered, even when [round] doesn't change (i.e. after a wrong tap).
         */
        val roundGeneration: Int = 0,
        /**
         * Best (lowest) ΔE correctly identified in this session so far.
         * Null until the player lands their first correct tap.
         */
        val sessionBestDeltaE: Float? = null,
    ) : ThresholdUiState
}
