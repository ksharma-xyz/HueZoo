package xyz.ksharma.huezoo.domain.color

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultColorEngine.generateMemoryPair] — the Color Memory Match
 * pair generator. Verifies the produced pair's ΔE against the real CIEDE2000
 * implementation (the design handoff mandates measuring, not approximating).
 */
class MemoryPairGenerationTest {

    @Test
    fun `same pair returns identical colors with zero deltaE`() {
        val pair = DefaultColorEngine(random = Random(SEED)).generateMemoryPair(
            targetDeltaE = 2.0f,
            isSame = true,
        )
        assertTrue(pair.isSame)
        assertEquals(pair.a, pair.b, "Same-pair must be pixel-identical")
        assertEquals(0f, pair.deltaE)
    }

    @Test
    fun `different pair hits target deltaE within tolerance across the full curve`() {
        val curve = listOf(5.0f, 4.0f, 3.0f, 2.5f, 2.0f, 1.5f, 1.2f, 1.0f, 0.7f, 0.5f)
        curve.forEachIndexed { index, target ->
            // Fresh seeded engine per target → unique base color + direction each time.
            val engine = DefaultColorEngine(random = Random(SEED + index))
            val pair = engine.generateMemoryPair(targetDeltaE = target, isSame = false)
            val actual = deltaE(pair.a.toLab(), pair.b.toLab())
            assertEquals(
                target,
                actual,
                TOLERANCE,
                "ΔE mismatch for target $target: actual $actual",
            )
        }
    }

    @Test
    fun `different pair colors are not equal`() {
        val pair = DefaultColorEngine(random = Random(SEED)).generateMemoryPair(
            targetDeltaE = 0.5f,
            isSame = false,
        )
        assertFalse(pair.isSame)
        assertNotEquals(pair.a, pair.b)
    }

    @Test
    fun `generateMemoryPair is deterministic for a fixed seed`() {
        val pair1 = DefaultColorEngine(random = Random(SEED)).generateMemoryPair(2.0f, isSame = false)
        val pair2 = DefaultColorEngine(random = Random(SEED)).generateMemoryPair(2.0f, isSame = false)
        assertEquals(pair1, pair2)
    }

    @Test
    fun `FakeColorEngine returns configured pair`() {
        val fake: ColorEngine = FakeColorEngine()
        val same = fake.generateMemoryPair(1.0f, isSame = true)
        assertTrue(same.isSame)
        assertEquals(same.a, same.b)
        val different = fake.generateMemoryPair(1.0f, isSame = false)
        assertFalse(different.isSame)
        assertNotEquals(different.a, different.b)
    }

    companion object {
        private const val SEED = 4242L

        // Slightly wider than the odd-swatch tolerance: the memory offset moves L*
        // too, so gamut clamping at conversion can nudge the measured ΔE a little.
        private const val TOLERANCE = 0.2f
    }
}
