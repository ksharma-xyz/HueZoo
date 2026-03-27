package xyz.ksharma.huezoo.ui.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [estimatedPerceptionTier].
 *
 * Pure function — no fakes or DI required.
 *
 * Tier boundaries (exclusive upper via `deltaE < deltaEMax`):
 * | Rank     | ΔE range        |
 * |----------|-----------------|
 * | TOP 1%   | ΔE < 0.5        |
 * | TOP 5%   | 0.5 ≤ ΔE < 1.0  |
 * | TOP 10%  | 1.0 ≤ ΔE < 1.5  |
 * | TOP 20%  | 1.5 ≤ ΔE < 2.0  |
 * | TOP 40%  | 2.0 ≤ ΔE < 3.0  |
 * | TOP 60%  | 3.0 ≤ ΔE < 4.0  |
 * | TOP 80%  | ΔE ≥ 4.0        |
 */
class PerceptionTierTest {

    // ─── TOP 1% ───────────────────────────────────────────────────────────────

    @Test
    fun top1pct_atZero() =
        assertEquals("TOP 1%", estimatedPerceptionTier(0f).rankLabel)

    @Test
    fun top1pct_justBelowBoundary() =
        assertEquals("TOP 1%", estimatedPerceptionTier(0.49f).rankLabel)

    // ─── TOP 5% ───────────────────────────────────────────────────────────────

    @Test
    fun top5pct_atBoundary() =
        assertEquals("TOP 5%", estimatedPerceptionTier(0.5f).rankLabel)

    @Test
    fun top5pct_justBelowBoundary() =
        assertEquals("TOP 5%", estimatedPerceptionTier(0.99f).rankLabel)

    // ─── TOP 10% ──────────────────────────────────────────────────────────────

    @Test
    fun top10pct_atBoundary() =
        assertEquals("TOP 10%", estimatedPerceptionTier(1.0f).rankLabel)

    @Test
    fun top10pct_justBelowBoundary() =
        assertEquals("TOP 10%", estimatedPerceptionTier(1.49f).rankLabel)

    // ─── TOP 20% ──────────────────────────────────────────────────────────────

    @Test
    fun top20pct_atBoundary() =
        assertEquals("TOP 20%", estimatedPerceptionTier(1.5f).rankLabel)

    @Test
    fun top20pct_justBelowBoundary() =
        assertEquals("TOP 20%", estimatedPerceptionTier(1.99f).rankLabel)

    // ─── TOP 40% ──────────────────────────────────────────────────────────────

    @Test
    fun top40pct_atBoundary() =
        assertEquals("TOP 40%", estimatedPerceptionTier(2.0f).rankLabel)

    @Test
    fun top40pct_justBelowBoundary() =
        assertEquals("TOP 40%", estimatedPerceptionTier(2.99f).rankLabel)

    // ─── TOP 60% ──────────────────────────────────────────────────────────────

    @Test
    fun top60pct_atBoundary() =
        assertEquals("TOP 60%", estimatedPerceptionTier(3.0f).rankLabel)

    @Test
    fun top60pct_justBelowBoundary() =
        assertEquals("TOP 60%", estimatedPerceptionTier(3.99f).rankLabel)

    // ─── TOP 80% ──────────────────────────────────────────────────────────────

    @Test
    fun top80pct_atBoundary() =
        assertEquals("TOP 80%", estimatedPerceptionTier(4.0f).rankLabel)

    @Test
    fun top80pct_veryHighDeltaE() =
        assertEquals("TOP 80%", estimatedPerceptionTier(100f).rankLabel)
}
