package xyz.ksharma.huezoo.domain.game

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import xyz.ksharma.huezoo.domain.color.DefaultColorEngine
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultDailyGameEngine].
 *
 * Uses the real [DefaultColorEngine] with a seeded [Random] — no fakes needed.
 *
 * Key invariants under test:
 * - ΔE curve has exactly 6 values matching the published design spec.
 * - Each round's returned [GameRound.deltaE] matches `deltaECurve[roundIndex]`.
 * - `oddIndex` placement is deterministic per (date, roundIndex) — same for all players.
 * - Different dates produce different `oddIndex` placements (collision unlikely with 6 slots
 *   and varied seeds, confirmed empirically here).
 */
class DefaultDailyGameEngineTest {

    private val engine = DefaultDailyGameEngine(
        colorEngine = DefaultColorEngine(random = Random(SEED)),
    )

    // ─── ΔE curve spec ────────────────────────────────────────────────────────

    @Test
    fun `deltaECurve has exactly 6 rounds`() =
        assertEquals(6, engine.deltaECurve.size)

    @Test
    fun `deltaECurve matches design spec values`() {
        val expected = listOf(4.0f, 3.0f, 2.0f, 1.5f, 1.0f, 0.7f)
        assertEquals(expected, engine.deltaECurve)
    }

    @Test
    fun `deltaECurve is strictly decreasing`() {
        engine.deltaECurve.zipWithNext { a, b ->
            assertTrue(a > b, "Curve must be strictly decreasing: $a should be > $b")
        }
    }

    @Test
    fun `totalRounds equals deltaECurve size`() =
        assertEquals(engine.deltaECurve.size, engine.totalRounds)

    // ─── Round structure ──────────────────────────────────────────────────────

    @Test
    fun `generateRound returns exactly 6 swatches`() {
        val round = engine.generateRound(DATE, roundIndex = 0, baseColor = BASE_COLOR)
        assertEquals(6, round.swatches.size)
    }

    @Test
    fun `generateRound deltaE matches curve for each round index`() {
        engine.deltaECurve.forEachIndexed { index, expectedDe ->
            val round = engine.generateRound(DATE, roundIndex = index, baseColor = BASE_COLOR)
            assertEquals(
                expectedDe, round.deltaE,
                "Round $index: expected ΔE $expectedDe, got ${round.deltaE}",
            )
        }
    }

    @Test
    fun `oddIndex is within swatch list bounds`() {
        repeat(engine.totalRounds) { index ->
            val round = engine.generateRound(DATE, roundIndex = index, baseColor = BASE_COLOR)
            assertTrue(round.oddIndex in 0..5, "oddIndex ${round.oddIndex} out of range at round $index")
        }
    }

    @Test
    fun `all non-odd swatches equal base color`() {
        val round = engine.generateRound(DATE, roundIndex = 2, baseColor = BASE_COLOR)
        round.swatches.forEachIndexed { i, swatch ->
            if (i != round.oddIndex) {
                assertEquals(BASE_COLOR, swatch, "Swatch[$i] should equal base color")
            }
        }
    }

    // ─── Determinism ──────────────────────────────────────────────────────────

    @Test
    fun `same date and roundIndex always produces same oddIndex`() {
        fun makeEngine() = DefaultDailyGameEngine(DefaultColorEngine(random = Random(SEED)))
        val round1 = makeEngine().generateRound(DATE, roundIndex = 3, baseColor = BASE_COLOR)
        val round2 = makeEngine().generateRound(DATE, roundIndex = 3, baseColor = BASE_COLOR)
        assertEquals(round1.oddIndex, round2.oddIndex)
    }

    @Test
    fun `different dates produce different oddIndex for same roundIndex`() {
        // Run over multiple round indices to reduce false-negative probability
        val date1 = LocalDate(2026, 3, 1)
        val date2 = LocalDate(2026, 6, 15)
        var foundDifference = false
        repeat(engine.totalRounds) { index ->
            val r1 = engine.generateRound(date1, roundIndex = index, baseColor = BASE_COLOR)
            val r2 = engine.generateRound(date2, roundIndex = index, baseColor = BASE_COLOR)
            if (r1.oddIndex != r2.oddIndex) foundDifference = true
        }
        assertTrue(foundDifference, "Different dates should produce at least one different oddIndex")
    }

    @Test
    fun `different roundIndex on same date can differ in oddIndex`() {
        val rounds = List(engine.totalRounds) { index ->
            engine.generateRound(DATE, roundIndex = index, baseColor = BASE_COLOR).oddIndex
        }
        // Not all 6 round indices should produce the identical oddIndex
        assertNotEquals(1, rounds.toSet().size, "All rounds having same oddIndex would be suspicious")
    }

    companion object {
        private const val SEED = 77L
        private val DATE = LocalDate(2026, 3, 27)
        private val BASE_COLOR = Color(0.6f, 0.4f, 0.8f)
    }
}
