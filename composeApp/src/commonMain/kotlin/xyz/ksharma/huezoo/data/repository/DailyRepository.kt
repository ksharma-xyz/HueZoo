package xyz.ksharma.huezoo.data.repository

import kotlinx.datetime.LocalDate
import xyz.ksharma.huezoo.domain.game.model.DailyChallenge
import xyz.ksharma.huezoo.domain.game.model.PersonalBest

interface DailyRepository {

    /** Returns today's record if the player has already played, null otherwise. */
    suspend fun getChallenge(date: LocalDate): DailyChallenge?

    /** Marks today's challenge as completed with the given score. */
    suspend fun saveCompletion(date: LocalDate, score: Float)

    suspend fun getPersonalBest(): PersonalBest?

    suspend fun savePersonalBest(deltaE: Float, score: Int)

    /**
     * Counts consecutive completed days ending on [today].
     * Walks backwards from [today] until it finds a day with no completed record.
     */
    suspend fun getStreak(today: LocalDate): Int
}
