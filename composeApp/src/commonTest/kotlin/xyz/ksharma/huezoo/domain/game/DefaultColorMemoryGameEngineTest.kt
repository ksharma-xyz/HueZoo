package xyz.ksharma.huezoo.domain.game

import xyz.ksharma.huezoo.domain.color.DefaultColorEngine
import xyz.ksharma.huezoo.domain.color.FakeColorEngine
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultColorMemoryGameEngineTest {

    private fun engine(seed: Long = SEED) = DefaultColorMemoryGameEngine(
        colorEngine = DefaultColorEngine(random = Random(seed)),
        random = Random(seed),
    )

    @Test
    fun `has ten rounds with the handoff deltaE curve`() {
        val e = engine()
        assertEquals(10, e.totalRounds)
        assertEquals(
            listOf(5.0f, 4.0f, 3.0f, 2.5f, 2.0f, 1.5f, 1.2f, 1.0f, 0.7f, 0.5f),
            e.deltaECurve,
        )
        assertEquals(e.totalRounds, e.deltaECurve.size)
    }

    @Test
    fun `generateRound uses the curve deltaE for different pairs`() {
        // FakeColorEngine echoes the requested target back as ColorPair.deltaE.
        val e = DefaultColorMemoryGameEngine(
            colorEngine = FakeColorEngine(),
            random = Random(SEED),
        )
        (1..e.totalRounds).forEach { round ->
            val pair = e.generateRound(round)
            if (!pair.isSame) {
                assertEquals(e.deltaECurve[round - 1], pair.deltaE, "Wrong ΔE for round $round")
            } else {
                assertEquals(0f, pair.deltaE)
            }
        }
    }

    @Test
    fun `generateRound rejects out-of-range rounds`() {
        val e = engine()
        assertFailsWith<IllegalArgumentException> { e.generateRound(0) }
        assertFailsWith<IllegalArgumentException> { e.generateRound(11) }
    }

    @Test
    fun `same-different split is roughly balanced over many rounds`() {
        val e = engine()
        var sameCount = 0
        repeat(SAMPLE_COUNT) {
            if (e.generateRound(1).isSame) sameCount++
        }
        // 50/50 coin flip — with 200 samples, expect between 30% and 70%.
        assertTrue(
            sameCount in (SAMPLE_COUNT * 3 / 10)..(SAMPLE_COUNT * 7 / 10),
            "isSame count $sameCount/$SAMPLE_COUNT outside plausible range for a fair coin",
        )
    }

    @Test
    fun `seeded engine is deterministic`() {
        val rounds1 = (1..10).map { engine(seed = 7L).generateRound(it).isSame }
        val rounds2 = (1..10).map { engine(seed = 7L).generateRound(it).isSame }
        assertEquals(rounds1, rounds2)
    }

    private companion object {
        const val SEED = 99L
        const val SAMPLE_COUNT = 200
    }
}
