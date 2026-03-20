package xyz.ksharma.huezoo.ui.home.state

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Ready(
        val threshold: ThresholdCardData,
        val daily: DailyCardData,
        val isPaid: Boolean,
        val totalGems: Int,
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
)
