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

    // ─── Game Color Generation ────────────────────────────────────────────────

    /**
     * Generates a varied game color with a weighted distribution:
     * - 70% vivid (high saturation, punchy hue)
     * - 20% muted/pastel (medium saturation, earthy or dusty)
     * - 10% near-grey (low saturation — makes the game visually diverse)
     *
     * The hue wheel is always fully sampled so rounds feel noticeably different
     * (red, orange, teal, purple, grey…) rather than clustering on one region.
     */
    override fun randomVividColor(): Color {
        val hue = random.nextFloat() * 360f
        return when {
            random.nextFloat() < 0.10f -> {
                // Near-grey: saturation almost zero, mid lightness
                val saturation = random.nextFloat() * 0.12f
                val lightness = 0.28f + random.nextFloat() * 0.44f // 0.28–0.72
                hslToColor(hue, saturation, lightness)
            }
            random.nextFloat() < 0.25f -> {
                // Muted / pastel: moderate saturation
                val saturation = 0.20f + random.nextFloat() * 0.38f // 0.20–0.58
                val lightness = 0.35f + random.nextFloat() * 0.30f // 0.35–0.65
                hslToColor(hue, saturation, lightness)
            }
            else -> {
                // Vivid: high saturation, punchy
                val saturation = VIVID_SAT_MIN + random.nextFloat() * VIVID_SAT_RANGE
                val lightness = VIVID_LIG_MIN + random.nextFloat() * VIVID_LIG_RANGE
                hslToColor(hue, saturation, lightness)
            }
        }
    }

    override fun randomVividColorExcluding(excludeHue: Float, excludeHueWidth: Float): Color {
        val hue = randomHueExcluding(excludeHue, excludeHueWidth)
        return when {
            random.nextFloat() < 0.10f -> {
                val saturation = random.nextFloat() * 0.12f
                val lightness = 0.28f + random.nextFloat() * 0.44f
                hslToColor(hue, saturation, lightness)
            }
            random.nextFloat() < 0.25f -> {
                val saturation = 0.20f + random.nextFloat() * 0.38f
                val lightness = 0.35f + random.nextFloat() * 0.30f
                hslToColor(hue, saturation, lightness)
            }
            else -> {
                val saturation = VIVID_SAT_MIN + random.nextFloat() * VIVID_SAT_RANGE
                val lightness = VIVID_LIG_MIN + random.nextFloat() * VIVID_LIG_RANGE
                hslToColor(hue, saturation, lightness)
            }
        }
    }

    /**
     * Picks a random hue uniformly from [0, 360) **excluding** the band
     * `(center - halfWidth, center + halfWidth)` (wrapping mod 360).
     *
     * Strategy: the allowed region has width `360 - 2*halfWidth`. Map a uniform
     * random value in that range starting just past the end of the exclusion zone,
     * then rotate back into [0, 360).
     */
    private fun randomHueExcluding(center: Float, halfWidth: Float): Float {
        val clampedWidth = halfWidth.coerceIn(0f, 180f)
        val allowed = 360f - 2f * clampedWidth
        val offset = random.nextFloat() * allowed
        // Start angle = just past the "far" edge of the exclusion zone
        val startAngle = (center + clampedWidth + 360f) % 360f
        return (startAngle + offset) % 360f
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
            (date.month.ordinal + 1).toLong() * 100L +
            date.day.toLong()

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
