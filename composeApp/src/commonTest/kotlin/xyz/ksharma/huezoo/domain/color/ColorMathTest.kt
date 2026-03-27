package xyz.ksharma.huezoo.domain.color

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure color-math functions in [ColorMath.kt].
 *
 * These tests cover the sRGB ↔ CIELAB conversion pipeline and the CIEDE2000
 * delta E formula. No randomness, no I/O — purely deterministic.
 *
 * ## What is tested
 * - [rgbToLab] / [Color.toLab]: known sRGB → Lab values for red, green, blue, black, white.
 * - [Lab.toColor]: inverse pipeline preserves color within sRGB gamut.
 * - [deltaE]: identity (same color = 0), symmetry, and perceptual scale spot-checks.
 *
 * ## What is NOT tested here
 * - Randomness-dependent functions ([randomVividColor], [generateOddSwatch]) → see [ColorEngineTest].
 * - Game scoring / daily seeding → see [ColorEngineTest].
 *
 * ## Tolerances
 * CIEDE2000 intermediate calculations use Double precision. We compare Float results
 * with tolerance [LAB_TOLERANCE] for Lab components and [DE_TOLERANCE] for ΔE values.
 */
class ColorMathTest {

    // ─── sRGB → CIELAB ───────────────────────────────────────────────────────

    @Test
    fun `black converts to Lab 0 0 0`() {
        val lab = Color.Black.toLab()
        assertEquals(0f, lab.l, LAB_TOLERANCE)
        assertEquals(0f, lab.a, LAB_TOLERANCE)
        assertEquals(0f, lab.b, LAB_TOLERANCE)
    }

    @Test
    fun `white converts to Lab 100 0 0`() {
        val lab = Color.White.toLab()
        assertEquals(100f, lab.l, LAB_TOLERANCE)
        assertEquals(0f, lab.a, LAB_TOLERANCE)
        assertEquals(0f, lab.b, LAB_TOLERANCE)
    }

    @Test
    fun `pure red sRGB converts to expected Lab values`() {
        // sRGB (1, 0, 0) → Lab(53.23, 80.11, 67.22) per CIE standard
        val lab = Color.Red.toLab()
        assertEquals(53.23f, lab.l, LAB_TOLERANCE)
        assertEquals(80.11f, lab.a, LAB_TOLERANCE)
        assertEquals(67.22f, lab.b, LAB_TOLERANCE)
    }

    @Test
    fun `pure green sRGB converts to expected Lab values`() {
        // sRGB (0, 1, 0) → Lab(87.74, -86.18, 83.18) per CIE standard
        val lab = Color(0f, 1f, 0f).toLab()
        assertEquals(87.74f, lab.l, LAB_TOLERANCE)
        assertEquals(-86.18f, lab.a, LAB_TOLERANCE)
        assertEquals(83.18f, lab.b, LAB_TOLERANCE)
    }

    @Test
    fun `pure blue sRGB converts to expected Lab values`() {
        // sRGB (0, 0, 1) → Lab(32.30, 79.19, -107.86) per CIE standard
        val lab = Color.Blue.toLab()
        assertEquals(32.30f, lab.l, LAB_TOLERANCE)
        assertEquals(79.19f, lab.a, LAB_TOLERANCE)
        assertEquals(-107.86f, lab.b, LAB_TOLERANCE)
    }

    // ─── CIELAB → sRGB round-trip ─────────────────────────────────────────────

    @Test
    fun `round-trip black stays black`() {
        val original = Color.Black
        val roundTripped = original.toLab().toColor()
        assertColorEquals(original, roundTripped, RGB_TOLERANCE)
    }

    @Test
    fun `round-trip white stays white`() {
        val original = Color.White
        val roundTripped = original.toLab().toColor()
        assertColorEquals(original, roundTripped, RGB_TOLERANCE)
    }

    @Test
    fun `round-trip mid-grey stays mid-grey`() {
        val grey = Color(0.5f, 0.5f, 0.5f)
        val roundTripped = grey.toLab().toColor()
        assertColorEquals(grey, roundTripped, RGB_TOLERANCE)
    }

    @Test
    fun `round-trip vivid red stays red`() {
        val original = Color.Red
        val roundTripped = original.toLab().toColor()
        assertColorEquals(original, roundTripped, RGB_TOLERANCE)
    }

    // ─── CIEDE2000 deltaE ─────────────────────────────────────────────────────

    @Test
    fun `deltaE of same color is zero`() {
        val lab = Color.Red.toLab()
        assertEquals(0f, deltaE(lab, lab), DE_TOLERANCE)
    }

    @Test
    fun `deltaE is symmetric`() {
        val lab1 = Color.Red.toLab()
        val lab2 = Color.Blue.toLab()
        val de12 = deltaE(lab1, lab2)
        val de21 = deltaE(lab2, lab1)
        assertEquals(de12, de21, DE_TOLERANCE)
    }

    @Test
    fun `deltaE of black and white is approximately 100`() {
        val de = deltaE(Color.Black.toLab(), Color.White.toLab())
        // CIEDE2000 distance between black and white ≈ 100 (ΔL* dominates)
        assertTrue(de > 95f, "Expected ΔE(black, white) > 95, got $de")
        assertTrue(de <= 101f, "Expected ΔE(black, white) ≤ 101, got $de")
    }

    @Test
    fun `deltaE of identical mid-grey is zero`() {
        val grey = Color(0.5f, 0.5f, 0.5f).toLab()
        assertEquals(0f, deltaE(grey, grey), DE_TOLERANCE)
    }

    @Test
    fun `deltaE increases as colors diverge further`() {
        val base = Color(0.5f, 0.5f, 0.5f).toLab()
        val close = Lab(base.l, base.a + 5f, base.b)
        val far = Lab(base.l, base.a + 20f, base.b)

        val deClose = deltaE(base, close)
        val deFar = deltaE(base, far)

        assertTrue(deClose < deFar, "Expected ΔE(close) < ΔE(far), got $deClose vs $deFar")
    }

    @Test
    fun `deltaE below 1 indicates imperceptible difference`() {
        val base = Color.Red.toLab()
        // Tiny nudge in a* — should be sub-1 ΔE
        val nudged = Lab(base.l, base.a + 0.3f, base.b)
        val de = deltaE(base, nudged)
        assertTrue(de < 1f, "Expected ΔE < 1 for tiny nudge, got $de")
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun assertColorEquals(expected: Color, actual: Color, tolerance: Float) {
        assertEquals(expected.red, actual.red, tolerance, "red mismatch")
        assertEquals(expected.green, actual.green, tolerance, "green mismatch")
        assertEquals(expected.blue, actual.blue, tolerance, "blue mismatch")
    }

    companion object {
        private const val LAB_TOLERANCE = 0.5f   // ±0.5 Lab units — standard for float precision
        private const val DE_TOLERANCE = 0.01f   // ±0.01 ΔE — well within game needs
        private const val RGB_TOLERANCE = 0.01f  // ±1% per channel for round-trip
    }
}
