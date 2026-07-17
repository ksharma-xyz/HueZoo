package xyz.ksharma.huezoo.domain.game

import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.color.ColorPair
import kotlin.random.Random

/**
 * @param random Coin-flip source for same/different. Pass a seeded [Random] in tests.
 */
class DefaultColorMemoryGameEngine(
    private val colorEngine: ColorEngine,
    private val random: Random = Random.Default,
) : ColorMemoryGameEngine {

    override val totalRounds: Int = TOTAL_ROUNDS

    override val deltaECurve: List<Float> =
        listOf(5.0f, 3.5f, 2.0f, 1.0f, 0.5f)

    override fun generateRound(round: Int): ColorPair {
        require(round in 1..totalRounds) { "round $round out of range 1..$totalRounds" }
        val isSame = random.nextBoolean()
        return colorEngine.generateMemoryPair(
            targetDeltaE = deltaECurve[round - 1],
            isSame = isSame,
        )
    }

    private companion object {
        const val TOTAL_ROUNDS = 5
    }
}
