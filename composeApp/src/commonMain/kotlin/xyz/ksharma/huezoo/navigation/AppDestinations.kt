package xyz.ksharma.huezoo.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Splash : NavKey

@Serializable
data object EyeStrainNotice : NavKey

@Serializable
data object Home : NavKey

@Serializable
data object ThresholdGame : NavKey

@Serializable
data object DailyGame : NavKey

/** Nav destination for the result screen — carries only the game type for identity/color setup. */
@Serializable
data class Result(val gameId: String) : NavKey

@Serializable
data object Leaderboard : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object Upgrade : NavKey

@Serializable
data object Licenses : NavKey

sealed interface GameId {
    companion object {
        const val THRESHOLD = "threshold"
        const val DAILY = "daily"
    }
}
