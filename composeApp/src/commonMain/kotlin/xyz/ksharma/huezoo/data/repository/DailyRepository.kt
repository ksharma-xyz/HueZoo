package xyz.ksharma.huezoo.data.repository

import kotlinx.datetime.LocalDate
import xyz.ksharma.huezoo.domain.game.model.DailyChallenge
import xyz.ksharma.huezoo.domain.game.model.PersonalBest

interface DailyRepository {

    /** Returns today's record if the player has already played, null otherwise. */
    suspend fun getChallenge(date: LocalDate): DailyChallenge?

    /** Marks today's challenge as completed. */
    suspend fun saveCompletion(date: LocalDate)

    suspend fun getPersonalBest(): PersonalBest?

    /** Saves personal best if [roundsSurvived] exceeds the stored best. */
    suspend fun savePersonalBest(deltaE: Float, roundsSurvived: Int)

    /**
     * Counts consecutive completed days ending on [today].
     * Walks backwards from [today] until it finds a day with no completed record.
     */
    suspend fun getStreak(today: LocalDate): Int
}
