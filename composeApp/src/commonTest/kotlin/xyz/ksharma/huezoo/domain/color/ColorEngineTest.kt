package xyz.ksharma.huezoo.domain.color

import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultColorEngine] — the game-level color API.
 *
 * All tests use a **seeded** [Random] so results are deterministic and the test
 * suite never flakes. The seed value is arbitrary; what matters is that a fixed
 * seed always produces the same sequence.
 *
 * ## What is tested
 * - [DefaultColorEngine.randomVividColor]: output is within vivid bounds (sat/lig ranges).
 * - [DefaultColorEngine.generateOddSwatch]: actual CIEDE2000 ΔE is within tolerance of target.
 * - [DefaultColorEngine.seededColorForDate]: same date → same color; different dates → different colors.
 * - [FakeColorEngine]: implements [ColorEngine] and returns configured values.
 *
 * ## Design note
 * Tests use [DefaultColorEngine] directly (not via Koin) because unit tests should
 * not depend on a DI framework. Koin is integration-level wiring only.
 */
class ColorEngineTest {

    private val seededEngine = DefaultColorEngine(random = Random(SEED))

    // ─── randomVividColor ─────────────────────────────────────────────────────

    @Test
    fun `randomVividColor produces colors in vivid lightness range`() {
        repeat(SAMPLE_COUNT) {
            val color = seededEngine.randomVividColor()
            val (_, s, l) = color.toHsl()
            assertTrue(
                l in DefaultColorEngine.VIVID_LIG_MIN..(DefaultColorEngine.VIVID_LIG_MIN + DefaultColorEngine.VIVID_LIG_RANGE),
                "Lightness $l out of vivid range [${DefaultColorEngine.VIVID_LIG_MIN}, ${DefaultColorEngine.VIVID_LIG_MIN + DefaultColorEngine.VIVID_LIG_RANGE}]",
            )
            assertTrue(
                s in DefaultColorEngine.VIVID_SAT_MIN..(DefaultColorEngine.VIVID_SAT_MIN + DefaultColorEngine.VIVID_SAT_RANGE),
                "Saturation $s out of vivid range [${DefaultColorEngine.VIVID_SAT_MIN}, ${DefaultColorEngine.VIVID_SAT_MIN + DefaultColorEngine.VIVID_SAT_RANGE}]",
            )
        }
    }

    @Test
    fun `randomVividColor with same seed produces same color`() {
        val engine1 = DefaultColorEngine(random = Random(SEED))
        val engine2 = DefaultColorEngine(random = Random(SEED))
        assertEquals(engine1.randomVividColor(), engine2.randomVividColor())
    }

    @Test
    fun `randomVividColor with different seeds produces different colors`() {
        val engine1 = DefaultColorEngine(random = Random(1))
        val engine2 = DefaultColorEngine(random = Random(2))
        // Not guaranteed for all seeds but with high probability over many samples
        val colors1 = List(10) { engine1.randomVividColor() }
        val colors2 = List(10) { engine2.randomVividColor() }
        assertNotEquals(colors1, colors2)
    }

    // ─── generateOddSwatch ────────────────────────────────────────────────────

    @Test
    fun `generateOddSwatch produces color within deltaE tolerance of target`() {
        val targets = listOf(5.0f, 3.0f, 2.0f, 1.5f, 1.0f, 0.7f, 0.5f)
        val base = seededEngine.randomVividColor()
        val baseLab = base.toLab()

        targets.forEach { target ->
            val odd = seededEngine.generateOddSwatch(base, target)
            val actualDe = deltaE(baseLab, odd.toLab())
            assertEquals(
                target,
                actualDe,
                ODD_SWATCH_DE_TOLERANCE,
                "ΔE mismatch for target $target: actual $actualDe",
            )
        }
    }

    @Test
    fun `generateOddSwatch is different from base`() {
        val base = seededEngine.randomVividColor()
        val odd = seededEngine.generateOddSwatch(base, targetDeltaE = 2.0f)
        assertNotEquals(base, odd, "Odd swatch should differ from base")
    }

    @Test
    fun `generateOddSwatch with same seed is deterministic`() {
        val base = DefaultColorEngine(random = Random(SEED)).randomVividColor()
        val odd1 = DefaultColorEngine(random = Random(SEED + 1)).generateOddSwatch(base, 2.0f)
        val odd2 = DefaultColorEngine(random = Random(SEED + 1)).generateOddSwatch(base, 2.0f)
        assertEquals(odd1, odd2)
    }

    // ─── seededColorForDate ───────────────────────────────────────────────────

    @Test
    fun `seededColorForDate returns same color for same date`() {
        val date = LocalDate(2026, 3, 19)
        val engine = DefaultColorEngine()
        assertEquals(engine.seededColorForDate(date), engine.seededColorForDate(date))
    }

    @Test
    fun `seededColorForDate returns different color for adjacent dates`() {
        val engine = DefaultColorEngine()
        val today = LocalDate(2026, 3, 19)
        val tomorrow = LocalDate(2026, 3, 20)
        assertNotEquals(engine.seededColorForDate(today), engine.seededColorForDate(tomorrow))
    }

    @Test
    fun `seededColorForDate returns vivid colors`() {
        val engine = DefaultColorEngine()
        repeat(SAMPLE_COUNT) { i ->
            val date = LocalDate(2026, 1 + (i % 12), 1 + (i % 28))
            val color = engine.seededColorForDate(date)
            val (_, s, l) = color.toHsl()
            assertTrue(
                l in DefaultColorEngine.DAILY_LIG_MIN..(DefaultColorEngine.DAILY_LIG_MIN + DefaultColorEngine.DAILY_LIG_RANGE),
                "Lightness $l out of daily range for $date",
            )
            assertTrue(
                s in DefaultColorEngine.DAILY_SAT_MIN..(DefaultColorEngine.DAILY_SAT_MIN + DefaultColorEngine.DAILY_SAT_RANGE),
                "Saturation $s out of daily range for $date",
            )
        }
    }

    @Test
    fun `seededColorForDate is stable across engine instances`() {
        val date = LocalDate(2026, 6, 15)
        val result1 = DefaultColorEngine().seededColorForDate(date)
        val result2 = DefaultColorEngine().seededColorForDate(date)
        assertEquals(result1, result2, "Daily color must be stable — same for all users on same day")
    }

    // ─── FakeColorEngine contract ─────────────────────────────────────────────

    @Test
    fun `FakeColorEngine implements ColorEngine and returns configured colors`() {
        val fake: ColorEngine = FakeColorEngine(
            vividColor = androidx.compose.ui.graphics.Color.Red,
            oddSwatch = androidx.compose.ui.graphics.Color.Blue,
        )
        assertEquals(androidx.compose.ui.graphics.Color.Red, fake.randomVividColor())
        assertEquals(androidx.compose.ui.graphics.Color.Blue, fake.generateOddSwatch(androidx.compose.ui.graphics.Color.Red, 1.0f))
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Converts a Compose [androidx.compose.ui.graphics.Color] to (hue, saturation, lightness).
     * Used to verify vivid-range constraints without depending on internal engine code.
     */
    private fun androidx.compose.ui.graphics.Color.toHsl(): Triple<Float, Float, Float> {
        val r = red; val g = green; val b = blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        val d = max - min

        if (d == 0f) return Triple(0f, 0f, l)

        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> ((g - b) / d + (if (g < b) 6f else 0f)) / 6f * 360f
            g -> ((b - r) / d + 2f) / 6f * 360f
            else -> ((r - g) / d + 4f) / 6f * 360f
        }
        return Triple(h, s, l)
    }

    companion object {
        private const val SEED = 12345L
        private const val SAMPLE_COUNT = 20
        private const val ODD_SWATCH_DE_TOLERANCE = 0.1f   // ±0.1 ΔE — good enough for game needs
    }
}
