package xyz.ksharma.huezoo.testutil

import androidx.compose.ui.graphics.Color
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine
import xyz.ksharma.huezoo.domain.game.model.GameRound

/**
 * Deterministic [ThresholdGameEngine] for ViewModel tests.
 *
 * Always places the odd swatch at index 0 so tests can tap a known index.
 * [generateRound] returns exactly 6 swatches — 5 base + 1 odd at index 0.
 *
 * Usage:
 * - `SwatchTapped(0)` = correct tap
 * - `SwatchTapped(1)` = wrong tap
 */
class FakeThresholdGameEngine(
    private val baseColor: Color = Color(0xFF00E5FF),
    private val oddColor: Color = Color(0xFFFF2D78),
) : ThresholdGameEngine {

    override fun generateRound(baseColor: Color, deltaE: Float): GameRound {
        val swatches = MutableList(6) { baseColor }
        swatches[0] = oddColor
        return GameRound(swatches = swatches.toList(), oddIndex = 0, deltaE = deltaE)
    }
}
