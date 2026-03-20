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
    val score: Int = 0,
    val gemsEarned: Int = 0,
    val gemBreakdown: List<GemAward> = emptyList(),
)

@Serializable
data object Leaderboard

@Serializable
data object Settings

sealed interface GameId {
    companion object {
        const val THRESHOLD = "threshold"
        const val DAILY = "daily"
    }
}
