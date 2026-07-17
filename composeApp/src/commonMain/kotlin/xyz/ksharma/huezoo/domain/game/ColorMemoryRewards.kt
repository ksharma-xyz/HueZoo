package xyz.ksharma.huezoo.domain.game

/**
 * Pure gem-reward calculator for a completed Color Memory Match session.
 *
 * Kept free of ViewModel/IO concerns so the exact payout rules are unit-testable:
 * - +[GameRewardRates.MEMORY_CORRECT_ROUND] per correct round
 * - +[GameRewardRates.MEMORY_STREAK_3_BONUS] once per distinct streak that reaches 3
 * - +[GameRewardRates.MEMORY_STREAK_5_BONUS] once per distinct streak that reaches 5
 * - +[GameRewardRates.MEMORY_PERFECT_BONUS] for a perfect run (all rounds correct)
 */
object ColorMemoryRewards {

    private const val STREAK_THRESHOLD_3 = 3
    private const val STREAK_THRESHOLD_5 = 5

    data class Payout(
        val correctGems: Int,
        val streakBonusGems: Int,
        val perfectBonusGems: Int,
    ) {
        val total: Int get() = correctGems + streakBonusGems + perfectBonusGems
    }

    /**
     * @param results per-round outcomes in play order — `true` = correct.
     */
    fun calculate(results: List<Boolean>): Payout {
        val correctCount = results.count { it }
        val correctGems = correctCount * GameRewardRates.MEMORY_CORRECT_ROUND

        // Streak bonuses: each maximal run of consecutive corrects earns each
        // threshold bonus at most once.
        var streakBonusGems = 0
        var run = 0
        for (correct in results) {
            if (correct) {
                run++
                if (run == STREAK_THRESHOLD_3) streakBonusGems += GameRewardRates.MEMORY_STREAK_3_BONUS
                if (run == STREAK_THRESHOLD_5) streakBonusGems += GameRewardRates.MEMORY_STREAK_5_BONUS
            } else {
                run = 0
            }
        }

        val perfectBonusGems = if (results.isNotEmpty() && correctCount == results.size) {
            GameRewardRates.MEMORY_PERFECT_BONUS
        } else {
            0
        }

        return Payout(
            correctGems = correctGems,
            streakBonusGems = streakBonusGems,
            perfectBonusGems = perfectBonusGems,
        )
    }

    /** Longest run of consecutive correct answers. */
    fun longestStreak(results: List<Boolean>): Int {
        var best = 0
        var run = 0
        for (correct in results) {
            run = if (correct) run + 1 else 0
            if (run > best) best = run
        }
        return best
    }
}
