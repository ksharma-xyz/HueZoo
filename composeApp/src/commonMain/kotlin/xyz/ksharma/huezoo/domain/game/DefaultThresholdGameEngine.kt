package xyz.ksharma.huezoo.domain.game

import androidx.compose.ui.graphics.Color
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.game.model.GameRound
import kotlin.random.Random

class DefaultThresholdGameEngine(
    private val colorEngine: ColorEngine,
    private val random: Random = Random.Default,
) : ThresholdGameEngine {

    override fun generateRound(baseColor: Color, deltaE: Float): GameRound {
        val oddColor = colorEngine.generateOddSwatch(baseColor, deltaE)
        val oddIndex = random.nextInt(SWATCH_COUNT)
        val swatches = List(SWATCH_COUNT) { i -> if (i == oddIndex) oddColor else baseColor }
        return GameRound(swatches = swatches, oddIndex = oddIndex, deltaE = deltaE)
    }

    private companion object {
        const val SWATCH_COUNT = 3
    }
}
