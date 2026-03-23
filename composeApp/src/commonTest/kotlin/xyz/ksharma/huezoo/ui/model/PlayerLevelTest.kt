package xyz.ksharma.huezoo.ui.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [PlayerLevel.fromGems].
 *
 * Pure logic — no fakes or DI required.
 *
 * ## What is tested
 * - Exact boundary values for each tier.
 * - Values just below each boundary stay in the previous tier.
 * - Values at and above each boundary promote to the next tier.
 *
 * ## Tier thresholds
 * | Tier    | Min gems |
 * |---------|----------|
 * | Rookie  |        0 |
 * | Trained |      150 |
 * | Sharp   |      750 |
 * | Elite   |    5 000 |
 * | Master  |   50 000 |
 */
class PlayerLevelTest {

    // ─── Rookie ───────────────────────────────────────────────────────────────

    @Test
    fun `0 gems is Rookie`() = assertEquals(PlayerLevel.Rookie, PlayerLevel.fromGems(0))

    @Test
    fun `149 gems is still Rookie`() = assertEquals(PlayerLevel.Rookie, PlayerLevel.fromGems(149))

    // ─── Trained ──────────────────────────────────────────────────────────────

    @Test
    fun `150 gems promotes to Trained`() = assertEquals(PlayerLevel.Trained, PlayerLevel.fromGems(150))

    @Test
    fun `749 gems is still Trained`() = assertEquals(PlayerLevel.Trained, PlayerLevel.fromGems(749))

    // ─── Sharp ────────────────────────────────────────────────────────────────

    @Test
    fun `750 gems promotes to Sharp`() = assertEquals(PlayerLevel.Sharp, PlayerLevel.fromGems(750))

    @Test
    fun `4999 gems is still Sharp`() = assertEquals(PlayerLevel.Sharp, PlayerLevel.fromGems(4_999))

    // ─── Elite ────────────────────────────────────────────────────────────────

    @Test
    fun `5000 gems promotes to Elite`() = assertEquals(PlayerLevel.Elite, PlayerLevel.fromGems(5_000))

    @Test
    fun `49999 gems is still Elite`() = assertEquals(PlayerLevel.Elite, PlayerLevel.fromGems(49_999))

    // ─── Master ───────────────────────────────────────────────────────────────

    @Test
    fun `50000 gems promotes to Master`() = assertEquals(PlayerLevel.Master, PlayerLevel.fromGems(50_000))

    @Test
    fun `large gem count stays Master`() = assertEquals(PlayerLevel.Master, PlayerLevel.fromGems(1_000_000))
}
