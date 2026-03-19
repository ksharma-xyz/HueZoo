package xyz.ksharma.huezoo.domain.game

import androidx.compose.ui.graphics.Color
import xyz.ksharma.huezoo.domain.game.model.GameRound

/**
 * Generates rounds for The Threshold game.
 *
 * Stateless — callers own all game progression state (current ΔE, round count, base colour).
 * Inject via Koin; never instantiate [DefaultThresholdGameEngine] directly in ViewModels.
 */
interface ThresholdGameEngine {

    /**
     * Generates a round at the given [deltaE] difficulty.
     *
     * Returns 3 swatches: 2 base colours and 1 odd colour shuffled into a random position.
     * The random seed is different every call so positions change between rounds.
     */
    fun generateRound(baseColor: Color, deltaE: Float): GameRound

    companion object {
        const val STARTING_DELTA_E = 5.0f
        const val DELTA_E_STEP = 0.3f
        const val MIN_DELTA_E = 0.1f
        const val MAX_ATTEMPTS = 5
    }
}
