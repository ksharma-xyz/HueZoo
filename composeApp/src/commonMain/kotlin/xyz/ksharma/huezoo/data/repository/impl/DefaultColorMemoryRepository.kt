package xyz.ksharma.huezoo.data.repository.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.ksharma.huezoo.data.db.HuezooDatabase
import xyz.ksharma.huezoo.data.repository.ColorMemoryRepository
import xyz.ksharma.huezoo.domain.game.model.PersonalBest
import xyz.ksharma.huezoo.navigation.GameId

class DefaultColorMemoryRepository(
    private val db: HuezooDatabase,
) : ColorMemoryRepository {

    override suspend fun getPersonalBest(): PersonalBest? = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getPersonalBest(GameId.COLOR_MEMORY).executeAsOneOrNull()?.let {
            PersonalBest(
                gameId = it.game_id,
                bestDeltaE = it.best_delta_e?.toFloat(),
                bestRounds = it.best_rounds?.toInt(),
            )
        }
    }

    override suspend fun savePersonalBest(score: Int, tightestDeltaE: Float) = withContext(Dispatchers.Default) {
        val q = db.huezooDatabaseQueries
        val current = q.getPersonalBest(GameId.COLOR_MEMORY).executeAsOneOrNull()
        val isNewBest = current?.best_rounds == null || score > current.best_rounds
        if (isNewBest) {
            q.upsertPersonalBest(GameId.COLOR_MEMORY, tightestDeltaE.toDouble(), score.toLong(), null)
        }
        Unit
    }
}
