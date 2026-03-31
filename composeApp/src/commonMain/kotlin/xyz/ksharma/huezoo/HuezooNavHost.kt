package xyz.ksharma.huezoo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.koinInject
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.EyeStrainNotice
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Home
import xyz.ksharma.huezoo.navigation.HuezooNavigator
import xyz.ksharma.huezoo.navigation.Leaderboard
import xyz.ksharma.huezoo.navigation.Licenses
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.navigation.Settings
import xyz.ksharma.huezoo.navigation.Splash
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.navigation.Upgrade
import xyz.ksharma.huezoo.navigation.huezooNavSerializationConfig
import xyz.ksharma.huezoo.navigation.rememberHuezooNavigator
import xyz.ksharma.huezoo.navigation.rememberNavigationState
import xyz.ksharma.huezoo.navigation.toEntries
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.ui.eyestrain.EyeStrainNoticeScreen
import xyz.ksharma.huezoo.ui.games.daily.DailyScreen
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdScreen
import xyz.ksharma.huezoo.ui.home.HomeScreen
import xyz.ksharma.huezoo.ui.leaderboard.LeaderboardScreen
import xyz.ksharma.huezoo.ui.licenses.LicensesScreen
import xyz.ksharma.huezoo.ui.result.ResultScreen
import xyz.ksharma.huezoo.ui.settings.SettingsScreen
import xyz.ksharma.huezoo.ui.splash.SplashScreen
import xyz.ksharma.huezoo.ui.upgrade.UpgradeScreen

/**
 * Root navigation host. Single back stack — no bottom nav tabs.
 *
 * Uses Navigation 3 with:
 * - [rememberNavigationState] for config-change-safe back stack persistence.
 * - [toEntries] to apply [rememberSaveableStateHolderNavEntryDecorator], preserving
 *   scroll position and rememberSaveable state when navigating back.
 * - [huezooNavSerializationConfig] for polymorphic serialization on iOS.
 */
@Suppress("RememberMissing")
@Composable
fun HuezooNavHost(modifier: Modifier = Modifier) {
    val navigationState = rememberNavigationState(
        startRoute = Splash,
        topLevelRoutes = setOf(Splash),
        serializationConfig = huezooNavSerializationConfig,
    )
    val navigator = rememberHuezooNavigator(navigationState)
    val entryProvider = huezooEntryProvider(navigator)

    // Calculate entries once to guard against an empty list on first composition
    // before Koin injection completes. @Suppress("RememberMissing") is intentional —
    // toEntries() is called twice to allow the isEmpty check before NavDisplay renders.
    val entries = navigationState.toEntries(entryProvider)

    if (entries.isNotEmpty()) {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.pop() },
            modifier = modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun huezooEntryProvider(navigator: HuezooNavigator): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        entry<Splash> {
            SplashScreen(
                // Replace Splash in-place — never leaves the back stack empty.
                onNavigateToHome = { navigator.replaceCurrent(Home) },
                onNavigateToEyeStrain = { navigator.replaceCurrent(EyeStrainNotice) },
            )
        }

        entry<EyeStrainNotice> {
            EyeStrainNoticeScreen(
                // Replace EyeStrainNotice in-place — never leaves the back stack empty.
                onNavigateToHome = { navigator.replaceCurrent(Home) },
            )
        }

        entry<Home> {
            HomeScreen(
                onNavigate = { navigator.goTo(it) },
                onSettingsTap = { navigator.goTo(Settings) },
                onUpgradeTap = { navigator.goTo(Upgrade) },
                onLeaderboardTap = { navigator.goTo(Leaderboard) },
            )
        }

        entry<Settings> {
            SettingsScreen(
                onBack = { navigator.pop() },
                onViewHealthNotice = { navigator.goTo(EyeStrainNotice) },
                onUpgrade = { navigator.goTo(Upgrade) },
                onLicenses = { navigator.goTo(Licenses) },
            )
        }

        entry<Licenses> {
            LicensesScreen(onBack = { navigator.pop() })
        }

        entry<Upgrade> {
            UpgradeScreen(onBack = { navigator.pop() })
        }

        entry<ThresholdGame> {
            ThresholdScreen(
                // Replace the game screen before pushing Result so the back stack
                // becomes [Home, Result] — not [Home, Game, Result].
                onResult = { navigator.navigateToResult(GameId.THRESHOLD) },
                onBack = { navigator.pop() },
            )
        }

        entry<DailyGame> {
            DailyScreen(
                onResult = { navigator.navigateToResult(GameId.DAILY) },
                onBack = { navigator.pop() },
            )
        }

        entry<Result> { key ->
            ResultScreen(
                gameId = key.gameId,
                onPlayAgain = {
                    navigator.pop() // remove Result
                    // Daily is once-per-day — go Home instead of replaying.
                    // Threshold: canPlayAgain gate is checked in ResultScreen;
                    // this lambda is only called when attempts are available.
                    if (key.gameId == GameId.THRESHOLD) {
                        navigator.goTo(ThresholdGame)
                    }
                },
                onBack = { navigator.pop() },
                onUpgradeTap = {
                    // Pop Result so the user can't back-navigate to it from Upgrade.
                    // Back stack becomes [Home, Upgrade] instead of [Home, Result, Upgrade].
                    navigator.pop()
                    navigator.goTo(Upgrade)
                },
            )
        }

        entry<Leaderboard> {
            val platformOps: PlatformOps = koinInject()
            LeaderboardScreen(
                onBack = { navigator.pop() },
                onShare = { text -> platformOps.shareText(text) },
            )
        }
    }
