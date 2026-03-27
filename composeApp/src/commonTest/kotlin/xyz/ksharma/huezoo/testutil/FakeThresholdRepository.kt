package xyz.ksharma.huezoo.testutil

import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.domain.game.model.PersonalBest
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * In-memory [ThresholdRepository] for ViewModel tests.
 *
 * Always reports [AttemptStatus.Available] with [ThresholdGameEngine.MAX_ATTEMPTS] so
 * ViewModels start sessions without hitting the 8-hour gate.
 *
 * Pass [initialBestDeltaE] to pre-populate a stored personal best — useful for
 * [ResultViewModel] isNewPersonalBest tests.
 *
 * Does not use [kotlinx.coroutines.Dispatchers.Default], so all operations
 * stay on the test dispatcher and virtual-time control works reliably.
 */
@OptIn(ExperimentalTime::class)
class FakeThresholdRepository(
    initialBestDeltaE: Float? = null,
) : ThresholdRepository {

    private var bestDeltaE: Float? = initialBestDeltaE
    private var attemptsUsed = 0

    override suspend fun getAttemptStatus(now: Instant): AttemptStatus =
        AttemptStatus.Available(
            attemptsUsed = 0,
            maxAttempts = ThresholdGameEngine.MAX_ATTEMPTS,
        )

    override suspend fun recordAttempt(now: Instant) { attemptsUsed++ }

    override suspend fun getPersonalBest(): PersonalBest? =
        bestDeltaE?.let { PersonalBest(gameId = "threshold", bestDeltaE = it, bestRounds = null) }

    override suspend fun savePersonalBest(deltaE: Float) {
        val current = bestDeltaE
        if (current == null || deltaE < current) bestDeltaE = deltaE
    }
}
