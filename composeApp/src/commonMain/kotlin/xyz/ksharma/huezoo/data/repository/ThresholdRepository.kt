package xyz.ksharma.huezoo.data.repository

import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.domain.game.model.PersonalBest
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface ThresholdRepository {

    /** Returns whether the player can start a game right now. */
    suspend fun getAttemptStatus(now: Instant): AttemptStatus

    /**
     * Records one attempt against the current 8-hour window.
     * Creates a new window if none is active.
     */
    suspend fun recordAttempt(now: Instant)

    suspend fun getPersonalBest(): PersonalBest?

    /** Saves personal best if [deltaE] is lower than the stored best. */
    suspend fun savePersonalBest(deltaE: Float)
}
