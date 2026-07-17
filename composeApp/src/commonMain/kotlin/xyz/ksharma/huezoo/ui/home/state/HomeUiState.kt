package xyz.ksharma.huezoo.ui.home.state

import xyz.ksharma.huezoo.ui.model.PlayerLevel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Ready(
        val threshold: ThresholdCardData,
        val daily: DailyCardData,
        val colorMemory: ColorMemoryCardData = ColorMemoryCardData(),
        val isPaid: Boolean,
        val totalGems: Int,
        val playerLevel: PlayerLevel,
        /** Player's display name — null until set in Settings. */
        val userName: String? = null,
        /** Consecutive days the player has completed the Daily Challenge. 0 until tracking is wired (UX.8). */
        val streak: Int = 0,
        /** Debug only — forces streak celebration animation regardless of real streak state. */
        val forceStreakCelebration: Boolean = false,
        /** Debug flag — when true, the in-development Color Memory Match card is shown. */
        val colorMemoryEnabled: Boolean = false,
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

data class ColorMemoryCardData(
    /** All-time best session score (−50..100). Null until the first completed session. */
    val bestScore: Int? = null,
)

@OptIn(ExperimentalTime::class)
data class DailyCardData(
    val isCompletedToday: Boolean,
    /** Non-null when completed today — the Instant at which the next puzzle unlocks (midnight local). */
    val nextPuzzleAt: Instant? = null,
    /** All-time best number of rounds correct (0–6). */
    val personalBestRounds: Int? = null,
)
