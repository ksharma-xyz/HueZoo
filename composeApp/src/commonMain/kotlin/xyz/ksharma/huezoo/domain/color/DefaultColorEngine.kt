package xyz.ksharma.huezoo.domain.color

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Production implementation of [ColorEngine].
 *
 * Registered as a singleton in Koin via [colorModule]. Inject via [ColorEngine],
 * never via this class directly.
 *
 * @param random Random number generator. Defaults to [Random.Default] in production.
 *   Pass a seeded [Random] in tests for deterministic, reproducible results.
 */
class DefaultColorEngine(private val random: Random = Random.Default) : ColorEngine {

    // ─── Vivid Color Generation ───────────────────────────────────────────────

    override fun randomVividColor(): Color {
        val hue = random.nextFloat() * 360f
        val saturation = VIVID_SAT_MIN + random.nextFloat() * VIVID_SAT_RANGE
        val lightness = VIVID_LIG_MIN + random.nextFloat() * VIVID_LIG_RANGE
        return hslToColor(hue, saturation, lightness)
    }

    // ─── Odd Swatch Generation ────────────────────────────────────────────────

    override fun generateOddSwatch(base: Color, targetDeltaE: Float): Color {
        val baseLab = base.toLab()

        // Pick a random direction in Lab a*/b* space
        val angle = random.nextFloat() * 2f * kotlin.math.PI.toFloat()
        val da = cos(angle)
        val db = sin(angle)

        // Binary search: find step magnitude s.t. CIEDE2000(base, candidate) ≈ targetDeltaE
        var low = 0f
        var high = ODD_SWATCH_SEARCH_HIGH

        repeat(ODD_SWATCH_BINARY_SEARCH_ITERATIONS) {
            val mid = (low + high) / 2f
            val candidate = Lab(baseLab.l, baseLab.a + da * mid, baseLab.b + db * mid)
            val de = deltaE(baseLab, candidate)
            if (de < targetDeltaE) low = mid else high = mid
        }

        val step = (low + high) / 2f
        return Lab(baseLab.l, baseLab.a + da * step, baseLab.b + db * step).toColor()
    }

    // ─── Deterministic Daily Color ────────────────────────────────────────────

    override fun seededColorForDate(date: LocalDate): Color {
        val seed = date.year.toLong() * 10_000L +
            date.monthNumber.toLong() * 100L +
            date.dayOfMonth.toLong()

        val hue = lcgHash(seed, multiplier = LCG_MULT_HUE, increment = LCG_INC_HUE)
            .let { abs(it) / LCG_MAX.toFloat() * 360f }

        val saturation = lcgHash(seed, multiplier = LCG_MULT_SAT, increment = LCG_INC_SAT)
            .let { DAILY_SAT_MIN + (abs(it) / LCG_MAX.toFloat()) * DAILY_SAT_RANGE }

        val lightness = lcgHash(seed, multiplier = LCG_MULT_LIG, increment = LCG_INC_LIG)
            .let { DAILY_LIG_MIN + (abs(it) / LCG_MAX.toFloat()) * DAILY_LIG_RANGE }

        return hslToColor(hue, saturation, lightness)
    }

    // ─── Scoring ──────────────────────────────────────────────────────────────

    override fun scoreFromDeltaE(de: Float): Int =
        (SCORE_BASE / de.coerceAtLeast(SCORE_MIN_DELTA_E)).toInt()

    // ─── Companion: constants ─────────────────────────────────────────────────

    companion object {
        // Vivid color generation bounds
        internal const val VIVID_SAT_MIN = 0.65f
        internal const val VIVID_SAT_RANGE = 0.35f // → 1.0
        internal const val VIVID_LIG_MIN = 0.30f
        internal const val VIVID_LIG_RANGE = 0.40f // → 0.70

        // Binary search for odd swatch — Lab a*/b* range is ±127; 160 gives headroom
        internal const val ODD_SWATCH_SEARCH_HIGH = 160f

        // 22 iterations → precision ~(160 / 2^22) ≈ 0.00004 Lab units
        internal const val ODD_SWATCH_BINARY_SEARCH_ITERATIONS = 22

        // LCG constants for daily seeded color (three independent streams)
        private const val LCG_MULT_HUE = 1_664_525L
        private const val LCG_INC_HUE = 1_013_904_223L
        private const val LCG_MULT_SAT = 22_695_477L
        private const val LCG_INC_SAT = 1L
        private const val LCG_MULT_LIG = 214_013L
        private const val LCG_INC_LIG = 2_531_011L
        private const val LCG_MAX = 0xFFFFFFFFL

        // Daily vivid bounds (slightly tighter than random, for visual consistency)
        internal const val DAILY_SAT_MIN = 0.70f
        internal const val DAILY_SAT_RANGE = 0.30f // → 1.0
        internal const val DAILY_LIG_MIN = 0.35f
        internal const val DAILY_LIG_RANGE = 0.30f // → 0.65

        // Score formula
        internal const val SCORE_BASE = 1000f
        internal const val SCORE_MIN_DELTA_E = 0.3f

        /** One LCG step — returns value in [0, LCG_MAX]. */
        internal fun lcgHash(seed: Long, multiplier: Long, increment: Long): Long =
            (seed * multiplier + increment) and LCG_MAX
    }
}

// ─── HSL → Color (package-internal helper, shared with tests) ────────────────

/**
 * Converts HSL components to a Compose [Color].
 *
 * @param h Hue in degrees [0, 360)
 * @param s Saturation [0, 1]
 * @param l Lightness [0, 1]
 */
internal fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
