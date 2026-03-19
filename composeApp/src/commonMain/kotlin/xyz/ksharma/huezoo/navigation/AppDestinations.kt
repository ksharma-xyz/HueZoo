package xyz.ksharma.huezoo.navigation

import kotlinx.serialization.Serializable

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
)

@Serializable
data object Leaderboard

sealed interface GameId {
    companion object {
        const val THRESHOLD = "threshold"
        const val DAILY = "daily"
    }
}
