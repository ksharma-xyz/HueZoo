package xyz.ksharma.huezoo.testutil

import xyz.ksharma.huezoo.data.repository.ColorMemoryRepository
import xyz.ksharma.huezoo.domain.game.model.PersonalBest
import xyz.ksharma.huezoo.navigation.GameId

/**
 * In-memory [ColorMemoryRepository] for tests.
 *
 * @param initialBestScore stored best score, or null for a fresh player.
 */
class FakeColorMemoryRepository(
    initialBestScore: Int? = null,
    initialTightestDeltaE: Float? = null,
) : ColorMemoryRepository {

    private var bestScore: Int? = initialBestScore
    private var tightestDeltaE: Float? = initialTightestDeltaE

    override suspend fun getPersonalBest(): PersonalBest? =
        bestScore?.let {
            PersonalBest(
                gameId = GameId.COLOR_MEMORY,
                bestDeltaE = tightestDeltaE,
                bestRounds = it,
            )
        }

    override suspend fun savePersonalBest(score: Int, tightestDeltaE: Float) {
        val current = bestScore
        if (current == null || score > current) {
            bestScore = score
            this.tightestDeltaE = tightestDeltaE
        }
    }
}
