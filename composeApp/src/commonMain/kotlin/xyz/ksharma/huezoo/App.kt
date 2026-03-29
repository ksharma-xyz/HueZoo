package xyz.ksharma.huezoo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import org.koin.compose.koinInject
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.ads.AdIds
import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.LocalHapticEngine
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.model.PlayerState
import xyz.ksharma.huezoo.ui.theme.HuezooTheme
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.LocalPlayerShelfColor

@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun App() {
    BasicAds.Initialize()
    val playerState: PlayerState = koinInject()
    val hapticEngine: HapticEngine = koinInject()
    val platformOps: PlatformOps = koinInject()
    AdIds.init(platformOps.isDebugBuild)

    HuezooTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // playerState.gems is Compose snapshot state — reading it here means App recomposes
            // whenever any ViewModel calls playerState.updateGems(), keeping the accent color live.
            val level = PlayerLevel.fromGems(playerState.gems)

            CompositionLocalProvider(
                LocalPlayerAccentColor provides level.levelColor,
                LocalPlayerShelfColor provides level.shelfColor,
                LocalHapticEngine provides hapticEngine,
            ) {
                HuezooNavHost()
            }
        }
    }
}
