package xyz.ksharma.huezoo.testutil

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import xyz.ksharma.huezoo.domain.game.DailyGameEngine
import xyz.ksharma.huezoo.domain.game.model.GameRound

/**
 * Deterministic [DailyGameEngine] for ViewModel tests.
 *
 * Always places the odd swatch at index 0 so tests can tap a known index.
 * Uses the provided [deltaECurve] for per-round difficulty — defaults to the
 * same curve documented in the game design (easy → hard).
 *
 * Usage:
 * - `SwatchTapped(0)` = correct tap (odd swatch)
 * - `SwatchTapped(1)` = wrong tap
 */
class FakeDailyGameEngine(
    override val totalRounds: Int = 6,
    override val deltaECurve: List<Float> = listOf(4.0f, 3.0f, 2.0f, 1.5f, 1.0f, 0.7f),
    private val baseColor: Color = Color(0xFF00E5FF),
    private val oddColor: Color = Color(0xFFFF2D78),
) : DailyGameEngine {

    override fun generateRound(date: LocalDate, roundIndex: Int, baseColor: Color): GameRound {
        val swatches = MutableList(6) { baseColor }
        swatches[0] = oddColor
        return GameRound(swatches = swatches.toList(), oddIndex = 0, deltaE = deltaECurve[roundIndex])
    }
}
