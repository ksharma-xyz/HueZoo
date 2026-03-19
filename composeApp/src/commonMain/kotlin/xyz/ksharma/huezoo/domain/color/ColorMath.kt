package xyz.ksharma.huezoo.domain.color

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ─── Constants ──────────────────────────────────────────────────────────────

private const val PI_D = kotlin.math.PI
private const val DEG_TO_RAD = PI_D / 180.0
private const val RAD_TO_DEG = 180.0 / PI_D

// XYZ D65 reference white (used in sRGB → XYZ matrix)
private const val REF_X = 0.95047
private const val REF_Y = 1.00000
private const val REF_Z = 1.08883

// sRGB linearization threshold
private const val SRGB_LINEARIZE_THRESHOLD = 0.04045
private const val SRGB_LINEARIZE_DIVISOR = 12.92
private const val SRGB_LINEARIZE_OFFSET = 0.055
private const val SRGB_LINEARIZE_SCALE = 1.055
private const val SRGB_GAMMA = 2.4

// Lab conversion constants
private const val LAB_F_THRESHOLD = 0.008856
private const val LAB_F_LINEAR_SCALE = 7.787
private const val LAB_F_OFFSET = 16.0 / 116.0
private const val LAB_L_SCALE = 116.0
private const val LAB_L_OFFSET = 16.0
private const val LAB_A_SCALE = 500.0
private const val LAB_B_SCALE = 200.0

// CIEDE2000 constants
private const val CIEDE_G_POWER = 7.0
private const val CIEDE_25_POW7 = 6103515625.0    // 25^7
private const val CIEDE_SL_WEIGHT = 0.015
private const val CIEDE_SC_WEIGHT = 0.045
private const val CIEDE_SH_WEIGHT = 0.015
private const val CIEDE_T_C1 = 0.17
private const val CIEDE_T_C2 = 0.24
private const val CIEDE_T_C3 = 0.32
private const val CIEDE_T_C4 = 0.20
private const val CIEDE_RT_HBAR_CENTER = 275.0
private const val CIEDE_RT_HBAR_WIDTH = 25.0
private const val CIEDE_RT_DTHETA_SCALE = 30.0

// ─── sRGB → CIELAB ──────────────────────────────────────────────────────────

/**
 * Converts a Compose [Color] (sRGB) to [Lab] (CIELAB, D65 illuminant).
 *
 * Pipeline: sRGB → linear RGB → XYZ → Lab.
 * Compose [Color.red]/[Color.green]/[Color.blue] return sRGB float components in [0,1].
 */
fun Color.toLab(): Lab = rgbToLab(red.toDouble(), green.toDouble(), blue.toDouble())

/**
 * Converts sRGB components (each in [0,1]) to [Lab].
 */
fun rgbToLab(r: Double, g: Double, b: Double): Lab {
    // Step 1: Inverse sRGB gamma — linearise each channel
    val rl = srgbLinearize(r)
    val gl = srgbLinearize(g)
    val bl = srgbLinearize(b)

    // Step 2: Linear RGB → XYZ (IEC 61966-2-1 D65 matrix), normalised by white point
    val x = (rl * 0.4124564 + gl * 0.3575761 + bl * 0.1804375) / REF_X
    val y = (rl * 0.2126729 + gl * 0.7151522 + bl * 0.0721750) / REF_Y
    val z = (rl * 0.0193339 + gl * 0.1191920 + bl * 0.9503041) / REF_Z

    // Step 3: XYZ → CIELAB
    val fx = labF(x)
    val fy = labF(y)
    val fz = labF(z)

    val l = (LAB_L_SCALE * fy - LAB_L_OFFSET).toFloat()
    val a = (LAB_A_SCALE * (fx - fy)).toFloat()
    val bVal = (LAB_B_SCALE * (fy - fz)).toFloat()

    return Lab(l, a, bVal)
}

/** Inverse sRGB gamma (linearise a single channel). */
private fun srgbLinearize(c: Double): Double =
    if (c <= SRGB_LINEARIZE_THRESHOLD) {
        c / SRGB_LINEARIZE_DIVISOR
    } else {
        ((c + SRGB_LINEARIZE_OFFSET) / SRGB_LINEARIZE_SCALE).pow(SRGB_GAMMA)
    }

/** CIELAB f() function (cube root or linear approximation below threshold). */
private fun labF(t: Double): Double =
    if (t > LAB_F_THRESHOLD) t.pow(1.0 / 3.0) else LAB_F_LINEAR_SCALE * t + LAB_F_OFFSET

// ─── CIELAB → sRGB ──────────────────────────────────────────────────────────

/**
 * Converts [Lab] back to a Compose [Color] (sRGB).
 * Values outside the sRGB gamut are clamped to [0,1].
 */
fun Lab.toColor(): Color {
    // Lab → XYZ
    val fy = (l + LAB_L_OFFSET) / LAB_L_SCALE
    val fx = (a / LAB_A_SCALE) + fy
    val fz = fy - (b / LAB_B_SCALE)

    val x = labFInverse(fx) * REF_X
    val y = labFInverse(fy) * REF_Y
    val z = labFInverse(fz) * REF_Z

    // XYZ → linear RGB (inverse D65 matrix)
    val rl =  x *  3.2404542 + y * -1.5371385 + z * -0.4985314
    val gl =  x * -0.9692660 + y *  1.8760108 + z *  0.0415560
    val bl =  x *  0.0556434 + y * -0.2040259 + z *  1.0572252

    // Linear → sRGB gamma encode, clamp to [0,1]
    val r = srgbDelinearize(rl).coerceIn(0.0, 1.0).toFloat()
    val g = srgbDelinearize(gl).coerceIn(0.0, 1.0).toFloat()
    val bClamped = srgbDelinearize(bl).coerceIn(0.0, 1.0).toFloat()

    return Color(r, g, bClamped)
}

/** Inverse of [labF] — used when converting Lab → XYZ. */
private fun labFInverse(t: Double): Double {
    val tCubed = t * t * t
    return if (tCubed > LAB_F_THRESHOLD) tCubed else (t - LAB_F_OFFSET) / LAB_F_LINEAR_SCALE
}

/** sRGB gamma encode (linearise → sRGB). */
private fun srgbDelinearize(c: Double): Double =
    if (c <= 0.0031308) {
        SRGB_LINEARIZE_DIVISOR * c
    } else {
        SRGB_LINEARIZE_SCALE * c.pow(1.0 / SRGB_GAMMA) - SRGB_LINEARIZE_OFFSET
    }

// ─── CIEDE2000 ──────────────────────────────────────────────────────────────

/**
 * Computes the CIEDE2000 color difference (ΔE₀₀) between two [Lab] colors.
 *
 * CIEDE2000 is the gold standard perceptual color difference formula.
 * ΔE < 1.0 is imperceptible to most people.
 * ΔE ~1.0–2.0 is just noticeable.
 * ΔE > 3.0 is clearly visible.
 *
 * Reference: Sharma et al. (2005), "The CIEDE2000 Color‐Difference Formula".
 */
@Suppress("LongMethod")
fun deltaE(lab1: Lab, lab2: Lab): Float {
    val l1 = lab1.l.toDouble()
    val a1 = lab1.a.toDouble()
    val b1 = lab1.b.toDouble()
    val l2 = lab2.l.toDouble()
    val a2 = lab2.a.toDouble()
    val b2 = lab2.b.toDouble()

    // Step 1: Adjust a* to account for CIE94 weighting (Sharma eq. 6–10)
    val c1ab = sqrt(a1 * a1 + b1 * b1)
    val c2ab = sqrt(a2 * a2 + b2 * b2)
    val cAbBarPow7 = ((c1ab + c2ab) / 2.0).pow(CIEDE_G_POWER)
    val g = 0.5 * (1.0 - sqrt(cAbBarPow7 / (cAbBarPow7 + CIEDE_25_POW7)))
    val a1p = a1 * (1.0 + g)
    val a2p = a2 * (1.0 + g)

    // C' and h' (adjusted chroma and hue)
    val c1p = sqrt(a1p * a1p + b1 * b1)
    val c2p = sqrt(a2p * a2p + b2 * b2)
    val h1p = ciededeHp(b1, a1p)
    val h2p = ciededeHp(b2, a2p)

    // Step 2: ΔL', ΔC', ΔH'
    val dLp = l2 - l1
    val dCp = c2p - c1p
    val dhp = ciedeDhp(c1p, c2p, h1p, h2p)
    val dHp = 2.0 * sqrt(c1p * c2p) * sin(dhp / 2.0 * DEG_TO_RAD)

    // Step 3: Arithmetic means
    val lBarP = (l1 + l2) / 2.0
    val cBarP = (c1p + c2p) / 2.0
    val hBarP = ciededHBarP(c1p, c2p, h1p, h2p)

    // Weighting functions
    val t = ciededeT(hBarP)
    val sL = 1.0 + CIEDE_SL_WEIGHT * (lBarP - 50.0).pow(2.0) /
        sqrt(20.0 + (lBarP - 50.0).pow(2.0))
    val sC = 1.0 + CIEDE_SC_WEIGHT * cBarP
    val sH = 1.0 + CIEDE_SH_WEIGHT * cBarP * t

    // Rotation term R_T
    val rt = ciededeRt(cBarP, hBarP)

    // Final ΔE₀₀ (k_L = k_C = k_H = 1)
    return sqrt(
        (dLp / sL).pow(2.0) +
            (dCp / sC).pow(2.0) +
            (dHp / sH).pow(2.0) +
            rt * (dCp / sC) * (dHp / sH),
    ).toFloat()
}

/** h' (adjusted hue angle) in degrees [0, 360). Zero when both inputs are zero. */
private fun ciededeHp(b: Double, ap: Double): Double {
    if (b == 0.0 && ap == 0.0) return 0.0
    val h = atan2(b, ap) * RAD_TO_DEG
    return if (h < 0.0) h + 360.0 else h
}

/** Δh' (difference of hue angles) in degrees. */
private fun ciedeDhp(c1p: Double, c2p: Double, h1p: Double, h2p: Double): Double {
    if (c1p * c2p == 0.0) return 0.0
    val diff = h2p - h1p
    return when {
        abs(diff) <= 180.0 -> diff
        diff > 180.0 -> diff - 360.0
        else -> diff + 360.0
    }
}

/** h̄' (mean hue) in degrees. */
private fun ciededHBarP(c1p: Double, c2p: Double, h1p: Double, h2p: Double): Double {
    if (c1p * c2p == 0.0) return h1p + h2p
    val sum = h1p + h2p
    return when {
        abs(h1p - h2p) <= 180.0 -> sum / 2.0
        sum < 360.0 -> (sum + 360.0) / 2.0
        else -> (sum - 360.0) / 2.0
    }
}

/** T (hue weighting factor). */
private fun ciededeT(hBarP: Double): Double =
    1.0 -
        CIEDE_T_C1 * cos((hBarP - 30.0) * DEG_TO_RAD) +
        CIEDE_T_C2 * cos(2.0 * hBarP * DEG_TO_RAD) +
        CIEDE_T_C3 * cos((3.0 * hBarP + 6.0) * DEG_TO_RAD) -
        CIEDE_T_C4 * cos((4.0 * hBarP - 63.0) * DEG_TO_RAD)

/** R_T (rotation term to compensate for blue region hue-chroma interaction). */
private fun ciededeRt(cBarP: Double, hBarP: Double): Double {
    val cBarPPow7 = cBarP.pow(CIEDE_G_POWER)
    val rc = sqrt(cBarPPow7 / (cBarPPow7 + CIEDE_25_POW7))
    val dTheta = CIEDE_RT_DTHETA_SCALE *
        exp(-((hBarP - CIEDE_RT_HBAR_CENTER) / CIEDE_RT_HBAR_WIDTH).pow(2.0))
    return -2.0 * rc * sin(2.0 * dTheta * DEG_TO_RAD)
}
