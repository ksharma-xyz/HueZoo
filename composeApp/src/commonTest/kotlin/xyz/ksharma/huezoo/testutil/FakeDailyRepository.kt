package xyz.ksharma.huezoo.testutil

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.domain.game.model.DailyChallenge
import xyz.ksharma.huezoo.domain.game.model.PersonalBest

/**
 * In-memory [DailyRepository] for ViewModel tests.
 *
 * Pass [initialBestRounds] / [initialBestDeltaE] to pre-populate a stored
 * personal best — useful for [ResultViewModel] isNewPersonalBest tests.
 *
 * Does not use [kotlinx.coroutines.Dispatchers.Default], so all operations
 * stay on the test dispatcher and virtual-time control works reliably.
 */
class FakeDailyRepository(
    initialBestRounds: Int? = null,
    initialBestDeltaE: Float? = null,
) : DailyRepository {

    private val completions = mutableSetOf<LocalDate>()
    private var bestRounds: Int? = initialBestRounds
    private var bestDeltaE: Float? = initialBestDeltaE

    override suspend fun getChallenge(date: LocalDate): DailyChallenge? =
        if (completions.contains(date)) DailyChallenge(date, completed = true) else null

    override suspend fun saveCompletion(date: LocalDate) { completions.add(date) }

    override suspend fun getPersonalBest(): PersonalBest? {
        if (bestDeltaE == null && bestRounds == null) return null
        return PersonalBest(gameId = "daily", bestDeltaE = bestDeltaE, bestRounds = bestRounds)
    }

    override suspend fun savePersonalBest(deltaE: Float, roundsSurvived: Int) {
        val current = bestRounds
        if (current == null || roundsSurvived > current) {
            bestRounds = roundsSurvived
            bestDeltaE = deltaE
        }
    }

    override suspend fun getStreak(today: LocalDate): Int {
        if (!completions.contains(today)) return 0
        var streak = 1
        var current = today
        while (true) {
            val prev = current.minus(1, DateTimeUnit.DAY)
            if (!completions.contains(prev)) break
            streak++
            current = prev
        }
        return streak
    }
}
