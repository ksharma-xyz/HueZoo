package xyz.ksharma.huezoo.domain.game.model

import kotlinx.datetime.Instant

/** Current state of the 8-hour attempt gate for The Threshold. */
sealed interface AttemptStatus {

    /** Player can start a game. */
    data class Available(
        val attemptsUsed: Int,
        val maxAttempts: Int,
    ) : AttemptStatus

    /** All attempts used — locked until the window resets. */
    data class Exhausted(
        val nextResetAt: Instant,
    ) : AttemptStatus
}
