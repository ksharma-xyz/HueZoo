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

/** Nav destination for the result screen — carries only the game type for identity/color setup. */
@Serializable
data class Result(val gameId: String)

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
