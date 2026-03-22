package xyz.ksharma.huezoo.data.repository.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import xyz.ksharma.huezoo.data.db.HuezooDatabase
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.domain.game.model.DailyChallenge
import xyz.ksharma.huezoo.domain.game.model.PersonalBest
import xyz.ksharma.huezoo.navigation.GameId

class DefaultDailyRepository(
    private val db: HuezooDatabase,
) : DailyRepository {

    override suspend fun getChallenge(date: LocalDate): DailyChallenge? = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getDailyChallenge(date.toString()).executeAsOneOrNull()?.let {
            DailyChallenge(
                date = LocalDate.parse(it.date),
                completed = it.completed != 0L,
            )
        }
    }

    override suspend fun saveCompletion(date: LocalDate) = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.upsertDailyChallenge(date.toString(), COMPLETED)
        Unit
    }

    override suspend fun getPersonalBest(): PersonalBest? = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getPersonalBest(GameId.DAILY).executeAsOneOrNull()?.let {
            PersonalBest(
                gameId = it.game_id,
                bestDeltaE = it.best_delta_e?.toFloat(),
                bestRounds = it.best_rounds?.toInt(),
            )
        }
    }

    override suspend fun savePersonalBest(deltaE: Float, roundsSurvived: Int) = withContext(Dispatchers.Default) {
        val q = db.huezooDatabaseQueries
        val current = q.getPersonalBest(GameId.DAILY).executeAsOneOrNull()
        val isNewBest = current?.best_rounds == null || roundsSurvived > current.best_rounds
        if (isNewBest) {
            q.upsertPersonalBest(GameId.DAILY, deltaE.toDouble(), roundsSurvived.toLong(), null)
        }
        Unit
    }

    override suspend fun getStreak(today: LocalDate): Int = withContext(Dispatchers.Default) {
        val completedDates = db.huezooDatabaseQueries.getCompletedDates()
            .executeAsList()
            .mapTo(HashSet()) { LocalDate.parse(it) }
        var streak = 0
        var checkDate = today
        while (completedDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minus(1, DateTimeUnit.DAY)
        }
        streak
    }

    private companion object {
        const val COMPLETED = 1L
    }
}
