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
        val bonusTries = settingsRepository.getBonusTries()
        // Available when base tries remain OR bonus tries are in the bank.
        //
        // Do NOT use (attemptsUsed < maxAttempts + bonusTries) — that formula is broken
        // because consuming a bonus try both increments attemptsUsed (+1) AND decrements
        // bonusTries (-1), shrinking effectiveMax and growing attemptsUsed simultaneously.
        // After an earn→play→earn cycle, attemptsUsed lands exactly at the new effectiveMax
        // (13 < 13 = false) even though bonusTries > 0 and another game is available.
        val hasBaseTriesLeft = attemptsUsed < maxAttempts
        val hasBonusTries = bonusTries > 0
        // Hearts display: base remaining, or bonus tries capped to heart count.
        val visualRemaining = when {
            hasBaseTriesLeft -> maxAttempts - attemptsUsed
            hasBonusTries -> minOf(bonusTries, maxAttempts)
            else -> 0
        }
        when {
            hasBaseTriesLeft || hasBonusTries -> AttemptStatus.Available(
                attemptsUsed = maxAttempts - visualRemaining,
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
        val attemptsUsedBefore = session?.attempts_used?.toInt() ?: 0
        if (session == null) {
            val nextResetAt = now.plus(WINDOW_DURATION)
            q.upsertThresholdSession(now.toString(), FIRST_ATTEMPT, nextResetAt.toString())
        } else {
            q.upsertThresholdSession(session.window_id, session.attempts_used + 1, session.next_reset_at)
        }
        // If this attempt was using a bonus try (exceeded the base cap), consume it.
        if (attemptsUsedBefore >= maxAttempts) {
            settingsRepository.consumeOneBonusTry()
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
