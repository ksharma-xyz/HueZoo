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
     * Returns 6 swatches: 5 base colours and 1 odd colour shuffled into a random position.
     * The 6 swatches map 1-to-1 to the petals of the flower layout (index 0 = top petal,
     * then clockwise at 60° intervals). The random seed is different every call.
     */
    fun generateRound(baseColor: Color, deltaE: Float): GameRound

    companion object {
        const val STARTING_DELTA_E = 5.0f
        const val DELTA_E_STEP = 0.3f
        const val MIN_DELTA_E = 0.1f

        const val MAX_ATTEMPTS_RELEASE = 10
        const val MAX_ATTEMPTS_DEBUG = 50

        fun maxAttempts(isDebug: Boolean) =
            if (isDebug) MAX_ATTEMPTS_DEBUG else MAX_ATTEMPTS_RELEASE
    }
}
