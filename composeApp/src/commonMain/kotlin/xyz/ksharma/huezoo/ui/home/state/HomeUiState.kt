package xyz.ksharma.huezoo.ui.home.state

import xyz.ksharma.huezoo.ui.model.PlayerLevel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Ready(
        val threshold: ThresholdCardData,
        val daily: DailyCardData,
        val isPaid: Boolean,
        val totalGems: Int,
        val playerLevel: PlayerLevel,
        /** Player's display name — null until set in Settings. */
        val userName: String? = null,
        /** Consecutive days the player has completed the Daily Challenge. 0 until tracking is wired (UX.8). */
        val streak: Int = 0,
        /** Global rank — null until Leaderboard / Firebase is integrated. */
        val rank: Int? = null,
    ) : HomeUiState
}

@OptIn(ExperimentalTime::class)
data class ThresholdCardData(
    val personalBestDeltaE: Float?,
    val attemptsRemaining: Int,
    val maxAttempts: Int,
    val isBlocked: Boolean,
    /** Non-null when blocked — the Instant at which attempts reset. */
    val nextResetAt: Instant? = null,
)

@OptIn(ExperimentalTime::class)
data class DailyCardData(
    val isCompletedToday: Boolean,
    val todayScore: Float?,
    /** Non-null when completed today — the Instant at which the next puzzle unlocks (midnight local). */
    val nextPuzzleAt: Instant? = null,
    /** All-time best score (maps to highest number of correct rounds). */
    val personalBestScore: Int? = null,
)
