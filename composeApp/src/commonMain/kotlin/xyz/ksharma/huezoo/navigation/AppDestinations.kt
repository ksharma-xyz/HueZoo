package xyz.ksharma.huezoo.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Splash

@Serializable
data object EyeStrainNotice

@Serializable
data object Home

@Serializable
data object ThresholdGame

@Serializable
data object DailyGame

@Serializable
data class Result(
    val gameId: String,
    val deltaE: Float,
    val roundsSurvived: Int,
    /** Correct rounds/taps across the full session (all tries for Threshold, all rounds for Daily). */
    val correctRounds: Int = 0,
    /** Total rounds/taps played across the full session. */
    val totalRounds: Int = 0,
    val gemsEarned: Int = 0,
    val gemBreakdown: List<GemAward> = emptyList(),
)

@Serializable
data object Leaderboard

@Serializable
data object Settings

@Serializable
data object Upgrade

sealed interface GameId {
    companion object {
        const val THRESHOLD = "threshold"
        const val DAILY = "daily"
    }
}
