package xyz.ksharma.huezoo.data.repository.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.ksharma.huezoo.data.db.HuezooDatabase
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.domain.game.model.PersonalBest
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.platform.PlatformOps
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DefaultThresholdRepository(
    private val db: HuezooDatabase,
    private val platformOps: PlatformOps,
    private val settingsRepository: SettingsRepository,
) : ThresholdRepository {

    private val maxAttempts: Int
        get() = ThresholdGameEngine.maxAttempts(platformOps.isDebugBuild)

    override suspend fun getAttemptStatus(now: Instant): AttemptStatus = withContext(Dispatchers.Default) {
        val q = db.huezooDatabaseQueries
        q.deleteExpiredSessions(now.toString())
        val session = q.getActiveThresholdSession(now.toString()).executeAsOneOrNull()
        val attemptsUsed = session?.attempts_used?.toInt() ?: 0
        when {
            attemptsUsed < maxAttempts -> AttemptStatus.Available(
                attemptsUsed = attemptsUsed,
                maxAttempts = maxAttempts,
            )
            settingsRepository.isPaid() -> {
                // Paid users: no cooldown — clear the exhausted session and give a fresh batch.
                q.deleteAllThresholdSessions()
                AttemptStatus.Available(attemptsUsed = 0, maxAttempts = maxAttempts)
            }
            else -> AttemptStatus.Exhausted(
                nextResetAt = Instant.parse(session!!.next_reset_at),
                maxAttempts = maxAttempts,
            )
        }
    }

    override suspend fun recordAttempt(now: Instant) = withContext(Dispatchers.Default) {
        val q = db.huezooDatabaseQueries
        q.deleteExpiredSessions(now.toString())
        val session = q.getActiveThresholdSession(now.toString()).executeAsOneOrNull()
        if (session == null) {
            val nextResetAt = now.plus(WINDOW_DURATION)
            q.upsertThresholdSession(now.toString(), FIRST_ATTEMPT, nextResetAt.toString())
        } else {
            q.upsertThresholdSession(session.window_id, session.attempts_used + 1, session.next_reset_at)
        }
        Unit
    }

    override suspend fun getPersonalBest(): PersonalBest? = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getPersonalBest(GameId.THRESHOLD).executeAsOneOrNull()?.let {
            PersonalBest(
                gameId = it.game_id,
                bestDeltaE = it.best_delta_e?.toFloat(),
                bestRounds = null,
            )
        }
    }

    override suspend fun savePersonalBest(deltaE: Float) = withContext(Dispatchers.Default) {
        val q = db.huezooDatabaseQueries
        val current = q.getPersonalBest(GameId.THRESHOLD).executeAsOneOrNull()
        val isNewBest = current?.best_delta_e == null || deltaE < current.best_delta_e
        if (isNewBest) {
            q.upsertPersonalBest(GameId.THRESHOLD, deltaE.toDouble(), null, null)
        }
        Unit
    }

    private companion object {
        val WINDOW_DURATION = 8.hours
        const val FIRST_ATTEMPT = 1L
    }
}
