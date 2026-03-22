package xyz.ksharma.huezoo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.EyeStrainNotice
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Home
import xyz.ksharma.huezoo.navigation.Leaderboard
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.navigation.Settings
import xyz.ksharma.huezoo.navigation.Splash
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.navigation.Upgrade
import xyz.ksharma.huezoo.ui.eyestrain.EyeStrainNoticeScreen
import xyz.ksharma.huezoo.ui.games.daily.DailyScreen
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdScreen
import xyz.ksharma.huezoo.ui.home.HomeScreen
import xyz.ksharma.huezoo.ui.leaderboard.LeaderboardScreen
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.result.ResultScreen
import xyz.ksharma.huezoo.ui.settings.SettingsScreen
import xyz.ksharma.huezoo.ui.splash.SplashScreen
import xyz.ksharma.huezoo.ui.upgrade.UpgradeScreen
import xyz.ksharma.huezoo.ui.theme.HuezooTheme
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.LocalPlayerShelfColor

@Composable
fun App() {
    val settingsRepository: SettingsRepository = koinInject()

    HuezooTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val backStack = remember { mutableStateListOf<Any>(Splash) }
            val scope = rememberCoroutineScope()

            // Re-read gems whenever the back stack changes (returning from a game screen).
            // PlayerLevel is derived from gems — drives the UI accent color everywhere.
            var gems by remember { mutableIntStateOf(0) }
            LaunchedEffect(backStack.size) { gems = settingsRepository.getGems() }
            val level = PlayerLevel.fromGems(gems)

            CompositionLocalProvider(
                LocalPlayerAccentColor provides level.levelColor,
                LocalPlayerShelfColor provides level.shelfColor,
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeLast() },
                    entryProvider = { destination ->
                        when (destination) {
                            is Splash -> NavEntry(destination) {
                                SplashScreen(
                                    onFinished = {
                                        backStack.removeLast() // drop Splash — can't go back to it
                                        scope.launch {
                                            val next = if (settingsRepository.hasSeenHealthNotice()) {
                                                Home
                                            } else {
                                                EyeStrainNotice
                                            }
                                            backStack.add(next)
                                        }
                                    },
                                )
                            }

                            is EyeStrainNotice -> NavEntry(destination) {
                                EyeStrainNoticeScreen(
                                    onDismiss = {
                                        scope.launch {
                                            settingsRepository.setSeenHealthNotice()
                                            backStack.removeLast() // drop EyeStrainNotice
                                            backStack.add(Home)
                                        }
                                    },
                                )
                            }

                            is Home -> NavEntry(destination) {
                                HomeScreen(
                                    onNavigate = { backStack.add(it) },
                                    onSettingsTap = { backStack.add(Settings) },
                                    onUpgradeTap = { backStack.add(Upgrade) },
                                    onLeaderboardTap = { backStack.add(Leaderboard) },
                                )
                            }

                            is Settings -> NavEntry(destination) {
                                SettingsScreen(
                                    onBack = { backStack.removeLast() },
                                    onViewHealthNotice = { backStack.add(EyeStrainNotice) },
                                )
                            }

                            is Upgrade -> NavEntry(destination) {
                                UpgradeScreen(
                                    onBack = { backStack.removeLast() },
                                )
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
                                val platformOps: xyz.ksharma.huezoo.platform.PlatformOps = koinInject()
                                LeaderboardScreen(
                                    onBack = { backStack.removeLast() },
                                    onShare = { text -> platformOps.shareText(text) },
                                )
                            }

                            else -> NavEntry(destination) {}
                        }
                    },
                )
            } // CompositionLocalProvider
        }
    }
}
