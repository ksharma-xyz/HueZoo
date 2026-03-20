package xyz.ksharma.huezoo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Home
import xyz.ksharma.huezoo.navigation.Leaderboard
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.navigation.Splash
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.ui.games.daily.DailyScreen
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdScreen
import xyz.ksharma.huezoo.ui.home.HomeScreen
import xyz.ksharma.huezoo.ui.leaderboard.LeaderboardScreen
import xyz.ksharma.huezoo.ui.result.ResultScreen
import xyz.ksharma.huezoo.ui.splash.SplashScreen
import xyz.ksharma.huezoo.ui.theme.HuezooTheme

@Composable
fun App() {
    HuezooTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val backStack = remember { mutableStateListOf<Any>(Splash) }

            NavDisplay(
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLast() },
                entryProvider = { destination ->
                    when (destination) {
                        is Splash -> NavEntry(destination) {
                            SplashScreen(
                                onFinished = {
                                    backStack.removeLast() // drop Splash — can't go back to it
                                    backStack.add(Home)
                                },
                            )
                        }

                        is Home -> NavEntry(destination) {
                            HomeScreen(onNavigate = { backStack.add(it) })
                        }

                        is ThresholdGame -> NavEntry(destination) {
                            ThresholdScreen(
                                // Remove the game screen before pushing Result so the
                                // backstack becomes [Home, Result] — not [Home, Game, Result].
                                // This prevents the stuck ViewModel from being resurfaced
                                // when the user presses Back from the Result screen.
                                onResult = { result ->
                                    backStack.removeLast()
                                    backStack.add(result)
                                },
                                onBack = { backStack.removeLast() },
                            )
                        }

                        is DailyGame -> NavEntry(destination) {
                            DailyScreen(
                                onResult = { result ->
                                    backStack.removeLast()
                                    backStack.add(result)
                                },
                                onBack = { backStack.removeLast() },
                            )
                        }

                        is Result -> NavEntry(destination) {
                            ResultScreen(
                                result = destination,
                                onPlayAgain = {
                                    backStack.removeLast() // remove Result
                                    // Daily is once-per-day — go Home instead of replaying.
                                    // Threshold: canPlayAgain gate is checked in ResultScreen;
                                    // this lambda is only called when attempts are available.
                                    if (destination.gameId == GameId.THRESHOLD) {
                                        backStack.add(ThresholdGame)
                                    }
                                },
                                onBack = { backStack.removeLast() },
                            )
                        }

                        is Leaderboard -> NavEntry(destination) {
                            LeaderboardScreen(onBack = { backStack.removeLast() })
                        }

                        else -> NavEntry(destination) {}
                    }
                },
            )
        }
    }
}
