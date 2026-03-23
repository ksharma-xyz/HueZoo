package xyz.ksharma.huezoo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.koinInject
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.LocalHapticEngine
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
import xyz.ksharma.huezoo.ui.model.PlayerState
import xyz.ksharma.huezoo.ui.result.ResultScreen
import xyz.ksharma.huezoo.ui.settings.SettingsScreen
import xyz.ksharma.huezoo.ui.splash.SplashScreen
import xyz.ksharma.huezoo.ui.upgrade.UpgradeScreen
import xyz.ksharma.huezoo.ui.theme.HuezooTheme
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.LocalPlayerShelfColor

@Composable
fun App() {
    val playerState: PlayerState = koinInject()
    val hapticEngine: HapticEngine = koinInject()

    HuezooTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val backStack = remember { mutableStateListOf<Any>(Splash) }

            // playerState.gems is Compose snapshot state — reading it here means App recomposes
            // whenever any ViewModel calls playerState.updateGems(), keeping the accent color live.
            val level = PlayerLevel.fromGems(playerState.gems)

            CompositionLocalProvider(
                LocalPlayerAccentColor provides level.levelColor,
                LocalPlayerShelfColor provides level.shelfColor,
                LocalHapticEngine provides hapticEngine,
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeLast() },
                    entryProvider = { destination ->
                        when (destination) {
                            is Splash -> NavEntry(destination) {
                                SplashScreen(
                                    // Replace Splash in-place — never leaves the backstack empty
                                    onNavigateToHome = { backStack[backStack.lastIndex] = Home },
                                    onNavigateToEyeStrain = { backStack[backStack.lastIndex] = EyeStrainNotice },
                                )
                            }

                            is EyeStrainNotice -> NavEntry(destination) {
                                EyeStrainNoticeScreen(
                                    // Replace EyeStrainNotice in-place — never leaves the backstack empty
                                    onNavigateToHome = { backStack[backStack.lastIndex] = Home },
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
