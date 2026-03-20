package xyz.ksharma.huezoo.domain.game

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.game.model.GameRound
import kotlin.random.Random

class DefaultDailyGameEngine(
    private val colorEngine: ColorEngine,
) : DailyGameEngine {

    override val totalRounds: Int = TOTAL_ROUNDS

    override val deltaECurve: List<Float> = listOf(4.0f, 3.0f, 2.0f, 1.5f, 1.0f, 0.7f)

    override fun generateRound(date: LocalDate, roundIndex: Int, baseColor: Color): GameRound {
        val deltaE = deltaECurve[roundIndex]
        val oddColor = colorEngine.generateOddSwatch(baseColor, deltaE)
        // Deterministic seed: same date + roundIndex → same shuffle for all players.
        val seed = date.toEpochDays().toLong() * SEED_ROUND_MULTIPLIER + roundIndex
        val oddIndex = Random(seed).nextInt(SWATCH_COUNT)
        val swatches = List(SWATCH_COUNT) { i -> if (i == oddIndex) oddColor else baseColor }
        return GameRound(swatches = swatches, oddIndex = oddIndex, deltaE = deltaE)
    }

    private companion object {
        const val TOTAL_ROUNDS = 6
        const val SWATCH_COUNT = 6
        const val SEED_ROUND_MULTIPLIER = 10L
    }
}
