package xyz.ksharma.huezoo.domain.game

import androidx.compose.ui.graphics.Color
import xyz.ksharma.huezoo.domain.color.DefaultColorEngine
import xyz.ksharma.huezoo.domain.color.deltaE
import xyz.ksharma.huezoo.domain.color.toLab
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultThresholdGameEngine].
 *
 * Uses the real [DefaultColorEngine] with a seeded [Random] — no fakes needed.
 * All tests are deterministic because both the color engine and the engine's
 * own random source are seeded.
 */
class DefaultThresholdGameEngineTest {

    private val colorEngine = DefaultColorEngine(random = Random(SEED))
    private val engine = DefaultThresholdGameEngine(colorEngine = colorEngine, random = Random(SEED))

    // ─── Round structure ──────────────────────────────────────────────────────

    @Test
    fun `generateRound returns exactly 6 swatches`() {
        val round = engine.generateRound(BASE_COLOR, deltaE = 2.0f)
        assertEquals(6, round.swatches.size)
    }

    @Test
    fun `oddIndex is within swatch list bounds`() {
        val round = engine.generateRound(BASE_COLOR, deltaE = 2.0f)
        assertTrue(round.oddIndex in 0..5, "oddIndex ${round.oddIndex} out of range 0..5")
    }

    @Test
    fun `swatch at oddIndex differs from base color`() {
        val round = engine.generateRound(BASE_COLOR, deltaE = 3.0f)
        assertNotEquals(
            BASE_COLOR,
            round.swatches[round.oddIndex],
            "Odd swatch must differ from base color",
        )
    }

    @Test
    fun `all swatches except oddIndex equal base color`() {
        val round = engine.generateRound(BASE_COLOR, deltaE = 2.0f)
        round.swatches.forEachIndexed { i, swatch ->
            if (i != round.oddIndex) {
                assertEquals(BASE_COLOR, swatch, "Swatch[$i] should equal base color")
            }
        }
    }

    @Test
    fun `deltaE in returned round matches requested deltaE`() {
        val targetDe = 2.0f
        val round = engine.generateRound(BASE_COLOR, deltaE = targetDe)
        assertEquals(targetDe, round.deltaE)
    }

    // ─── Difficulty curve ─────────────────────────────────────────────────────

    @Test
    fun `generated odd swatch ΔE is close to target across difficulty range`() {
        val colorEng = DefaultColorEngine(random = Random(42))
        val eng = DefaultThresholdGameEngine(colorEngine = colorEng, random = Random(42))
        val targets = listOf(5.0f, 3.0f, 2.0f, 1.5f, 1.0f, 0.5f, ThresholdGameEngine.MIN_DELTA_E)
        targets.forEach { target ->
            val round = eng.generateRound(BASE_COLOR, deltaE = target)
            val actualDe = deltaE(BASE_COLOR.toLab(), round.swatches[round.oddIndex].toLab())
            assertEquals(
                target, actualDe, DE_TOLERANCE,
                "ΔE mismatch for target $target: got $actualDe",
            )
        }
    }

    // ─── Constants ────────────────────────────────────────────────────────────

    @Test
    fun `STARTING_DELTA_E is 5_0`() = assertEquals(5.0f, ThresholdGameEngine.STARTING_DELTA_E)

    @Test
    fun `DELTA_E_STEP is 0_3`() = assertEquals(0.3f, ThresholdGameEngine.DELTA_E_STEP)

    @Test
    fun `MIN_DELTA_E is 0_1`() = assertEquals(0.1f, ThresholdGameEngine.MIN_DELTA_E)

    @Test
    fun `MAX_ATTEMPTS is 5`() = assertEquals(5, ThresholdGameEngine.MAX_ATTEMPTS)

    // ─── Determinism ──────────────────────────────────────────────────────────

    @Test
    fun `same seed produces same oddIndex`() {
        fun makeEngine() = DefaultThresholdGameEngine(
            colorEngine = DefaultColorEngine(random = Random(SEED)),
            random = Random(SEED),
        )
        val round1 = makeEngine().generateRound(BASE_COLOR, 2.0f)
        val round2 = makeEngine().generateRound(BASE_COLOR, 2.0f)
        assertEquals(round1.oddIndex, round2.oddIndex)
    }

    companion object {
        private const val SEED = 99L
        private val BASE_COLOR = Color(0.8f, 0.3f, 0.5f)
        private const val DE_TOLERANCE = 0.15f
    }
}
