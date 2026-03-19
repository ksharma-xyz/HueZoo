package xyz.ksharma.huezoo.data.repository

import kotlinx.datetime.Instant
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.domain.game.model.PersonalBest

interface ThresholdRepository {

    /** Returns whether the player can start a game right now. */
    suspend fun getAttemptStatus(now: Instant): AttemptStatus

    /**
     * Records one attempt against the current 8-hour window.
     * Creates a new window if none is active.
     */
    suspend fun recordAttempt(now: Instant)

    suspend fun getPersonalBest(): PersonalBest?

    suspend fun savePersonalBest(deltaE: Float, score: Int)
}
