package xyz.ksharma.huezoo.data.repository.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
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
                score = it.score.toFloat(),
                completed = it.completed != 0L,
            )
        }
    }

    override suspend fun saveCompletion(date: LocalDate, score: Float) = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.upsertDailyChallenge(date.toString(), score.toDouble(), COMPLETED)
        Unit
    }

    override suspend fun getPersonalBest(): PersonalBest? = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getPersonalBest(GameId.DAILY).executeAsOneOrNull()?.let {
            PersonalBest(
                gameId = it.game_id,
                bestDeltaE = it.best_delta_e?.toFloat(),
                bestScore = it.best_score?.toInt(),
            )
        }
    }

    override suspend fun savePersonalBest(deltaE: Float, score: Int) = withContext(Dispatchers.Default) {
        val q = db.huezooDatabaseQueries
        val current = q.getPersonalBest(GameId.DAILY).executeAsOneOrNull()
        val isNewBest = current?.best_score == null || score > current.best_score
        if (isNewBest) {
            q.upsertPersonalBest(GameId.DAILY, deltaE.toDouble(), score.toLong(), null)
        }
        Unit
    }

    private companion object {
        const val COMPLETED = 1L
    }
}
