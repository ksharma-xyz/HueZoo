package xyz.ksharma.huezoo.domain.game

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the Color Memory Match gem payout rules from the design handoff:
 * +2 per correct round, +5 at a 3-streak, +10 at a 5-streak (one-shot per
 * distinct streak), +25 perfect-run bonus.
 */
class ColorMemoryRewardsTest {

    @Test
    fun `all wrong pays nothing`() {
        val payout = ColorMemoryRewards.calculate(List(10) { false })
        assertEquals(0, payout.total)
    }

    @Test
    fun `each correct round pays 2 gems`() {
        // Correct on rounds 1, 3, 5, 7 — no streak ever reaches 3.
        val results = listOf(true, false, true, false, true, false, true, false, false, false)
        val payout = ColorMemoryRewards.calculate(results)
        assertEquals(8, payout.correctGems)
        assertEquals(0, payout.streakBonusGems)
        assertEquals(0, payout.perfectBonusGems)
    }

    @Test
    fun `streak of 3 pays one 5-gem bonus`() {
        val results = listOf(true, true, true, false, false, false, false, false, false, false)
        val payout = ColorMemoryRewards.calculate(results)
        assertEquals(6, payout.correctGems)
        assertEquals(GameRewardRates.MEMORY_STREAK_3_BONUS, payout.streakBonusGems)
    }

    @Test
    fun `streak of 5 pays both the 3-streak and 5-streak bonuses`() {
        val results = listOf(true, true, true, true, true, false, false, false, false, false)
        val payout = ColorMemoryRewards.calculate(results)
        assertEquals(
            GameRewardRates.MEMORY_STREAK_3_BONUS + GameRewardRates.MEMORY_STREAK_5_BONUS,
            payout.streakBonusGems,
        )
    }

    @Test
    fun `two distinct streaks each earn their own bonus`() {
        // 3-streak, wrong, 3-streak, wrong ×3 → two separate +5 bonuses.
        val results = listOf(true, true, true, false, true, true, true, false, false, false)
        val payout = ColorMemoryRewards.calculate(results)
        assertEquals(GameRewardRates.MEMORY_STREAK_3_BONUS * 2, payout.streakBonusGems)
    }

    @Test
    fun `perfect run pays correct plus streak plus perfect bonuses`() {
        val payout = ColorMemoryRewards.calculate(List(10) { true })
        assertEquals(20, payout.correctGems) // 10 × 2
        assertEquals(
            GameRewardRates.MEMORY_STREAK_3_BONUS + GameRewardRates.MEMORY_STREAK_5_BONUS,
            payout.streakBonusGems,
        )
        assertEquals(GameRewardRates.MEMORY_PERFECT_BONUS, payout.perfectBonusGems)
        assertEquals(60, payout.total)
    }

    @Test
    fun `empty results never counts as perfect`() {
        val payout = ColorMemoryRewards.calculate(emptyList())
        assertEquals(0, payout.total)
    }

    @Test
    fun `longestStreak finds the maximal run`() {
        assertEquals(0, ColorMemoryRewards.longestStreak(List(5) { false }))
        assertEquals(10, ColorMemoryRewards.longestStreak(List(10) { true }))
        assertEquals(
            4,
            ColorMemoryRewards.longestStreak(
                listOf(true, true, false, true, true, true, true, false, true, false),
            ),
        )
    }
}
