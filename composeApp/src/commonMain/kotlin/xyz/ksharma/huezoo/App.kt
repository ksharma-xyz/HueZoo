package xyz.ksharma.huezoo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.NavDisplay
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.Home
import xyz.ksharma.huezoo.navigation.Leaderboard
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.ui.games.daily.DailyScreen
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdScreen
import xyz.ksharma.huezoo.ui.home.HomeScreen
import xyz.ksharma.huezoo.ui.leaderboard.LeaderboardScreen
import xyz.ksharma.huezoo.ui.result.ResultScreen
import xyz.ksharma.huezoo.ui.theme.HuezooTheme

@Composable
fun App() {
    HuezooTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val backstack = remember { mutableStateListOf<Any>(Home) }

            NavDisplay(
                backstack = backstack,
                onBack = { if (backstack.size > 1) backstack.removeLast() },
            ) { entry ->
                when (val destination = entry.key) {
                    is Home -> HomeScreen(
                        onNavigate = { backstack.add(it) },
                    )

                    is ThresholdGame -> ThresholdScreen(
                        onResult = { backstack.add(it) },
                        onBack = { backstack.removeLast() },
                    )

                    is DailyGame -> DailyScreen(
                        onResult = { backstack.add(it) },
                        onBack = { backstack.removeLast() },
                    )

                    is Result -> ResultScreen(
                        result = destination,
                        onLeaderboard = { backstack.add(Leaderboard) },
                        onPlayAgain = {
                            backstack.removeAll { it is Result }
                            backstack.add(ThresholdGame)
                        },
                        onBack = { backstack.removeLast() },
                    )

                    is Leaderboard -> LeaderboardScreen(
                        onBack = { backstack.removeLast() },
                    )
                }
            }
        }
    }
}
