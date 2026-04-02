package xyz.ksharma.huezoo.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.BannerAd
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.debug.DebugFlags
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.platform.ads.AdIds
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.DeltaEWorldRankSheet
import xyz.ksharma.huezoo.ui.components.HuezooBottomSheet
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.LevelsProgressSheet
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.HomeUiEvent
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.paywall.PaywallSheet
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val CHALLENGE_NAMES = listOf(
    "ULTRAVIOLET BURST", "CRIMSON SURGE", "AZURE DRIFT", "MAGENTA STORM",
    "COBALT PULSE", "EMERALD WAVE", "SOLAR FLARE", "NEON BREACH",
    "VOID ECHO", "AMBER SHIFT", "CORAL STRIKE", "INDIGO TIDE",
    "SCARLET HAZE", "TEAL PHANTOM", "GOLD NOVA", "CERULEAN PEAK",
    "MAUVE IMPACT", "JADE SIGNAL", "RUBY FLASH", "CYAN PROTOCOL",
    "VIOLET APEX", "BRONZE ECHO", "TITANIUM RAY", "ONYX WAVE",
    "PRISM SURGE", "INFRA PULSE", "ULTRA DRIFT", "PLASMA STRIKE",
    "QUARTZ BLOOM", "OMEGA FLASH", "DELTA SURGE", "HELIOS BURST",
)

@OptIn(ExperimentalTime::class)
@Composable
fun HomeScreen(
    onNavigate: (NavKey) -> Unit,
    onSettingsTap: () -> Unit,
    onUpgradeTap: () -> Unit,
    onLeaderboardTap: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onUiEvent(HomeUiEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlowBackground(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                HomeUiState.Loading -> Unit
                is HomeUiState.Ready -> ReadyContent(
                    state = state,
                    onThresholdTap = { onNavigate(ThresholdGame) },
                    onDailyTap = { onNavigate(DailyGame) },
                    onSettingsTap = onSettingsTap,
                    onUpgradeTap = onUpgradeTap,
                    onLeaderboardTap = onLeaderboardTap,
                    onTryGranted = { viewModel.onUiEvent(HomeUiEvent.ScreenResumed) },
                )
            }
        }

        // Banner: always visible, no animation delay, centered so it looks clean on tablets too.
        // Matches the same simple pattern used on ThresholdScreen / DailyScreen.
        val readyState = uiState as? HomeUiState.Ready
        @OptIn(DependsOnGoogleMobileAds::class)
        if (readyState != null && !readyState.isPaid && !DebugFlags.hideAds) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                BannerAd(adUnitId = AdIds.banner)
            }
        }
    }
}

@OptIn(ExperimentalTime::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    onThresholdTap: () -> Unit,
    onDailyTap: () -> Unit,
    onSettingsTap: () -> Unit,
    onUpgradeTap: () -> Unit,
    onLeaderboardTap: () -> Unit,
    onTryGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLevelsSheet by remember { mutableStateOf(false) }
    var showDeltaESheet by remember { mutableStateOf(false) }
    var showPaywallSheet by remember { mutableStateOf(false) }

    HomeSheetOverlays(
        showPaywallSheet = showPaywallSheet,
        showLevelsSheet = showLevelsSheet,
        showDeltaESheet = showDeltaESheet,
        totalGems = state.totalGems,
        deltaE = state.threshold.personalBestDeltaE,
        onPaywallDismiss = {
            showPaywallSheet = false
            onTryGranted()
        },
        onPaywallUpgrade = {
            showPaywallSheet = false
            onUpgradeTap()
        },
        onLevelsDismiss = { showLevelsSheet = false },
        onDeltaEDismiss = { showDeltaESheet = false },
    )

    val challengeName = remember {
        val dayOfYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfYear
        CHALLENGE_NAMES[dayOfYear % CHALLENGE_NAMES.size]
    }

    // Only celebrate when the streak *increases* this session — not every time the screen loads.
    val prevStreak = remember { mutableStateOf(-1) }
    val shouldCelebrate = remember { mutableStateOf(false) }
    LaunchedEffect(state.streak) {
        val prev = prevStreak.value
        if (prev >= 0 && state.streak > prev) {
            shouldCelebrate.value = true
            delay(5_400L) // outlast shimmerCelebration's 5 000 ms + fade-out
            shouldCelebrate.value = false
        }
        prevStreak.value = state.streak
    }
    val isStreakCelebrating = state.forceStreakCelebration || shouldCelebrate.value

    // ── Cold-open greeting animation (skipped on re-entry) ────────────────────
    val alreadyPlayed = remember { HomeScreenAnimationState.hasPlayed }
    var displayedWelcome by remember { mutableStateOf(if (alreadyPlayed) "WELCOME," else "") }

    LaunchedEffect(Unit) {
        if (!alreadyPlayed) {
            delay(GREETING_START_DELAY_MS)
            "WELCOME,".forEach { char ->
                delay(GREETING_TYPEWRITER_MS)
                displayedWelcome += char
            }
        }
    }

    // Mark session done → re-entries skip all entrance animations
    LaunchedEffect(Unit) {
        if (!alreadyPlayed) {
            delay(SESSION_ANIM_DONE_MS)
            HomeScreenAnimationState.hasPlayed = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HuezooTopBar(onSettingsClick = onSettingsTap)

        Column(modifier = Modifier.padding(horizontal = HuezooSpacing.md)) {
            Spacer(Modifier.height(HuezooSpacing.md))

            HuezooLabelSmall(
                text = displayedWelcome,
                color = HuezooColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            AnimatedCallsign(
                text = if (state.userName != null) state.userName.uppercase() else "AGENT",
                color = LocalPlayerAccentColor.current,
                animate = !alreadyPlayed,
                startDelay = CALLSIGN_DELAY_MS,
                onClick = onSettingsTap,
            )

            Spacer(Modifier.height(HuezooSpacing.md))

            StaggeredCard(index = 0) {
                StatsSection(
                    totalGems = state.totalGems,
                    streak = state.streak,
                    isStreakCelebrating = isStreakCelebrating,
                    onGemsClick = { showLevelsSheet = true },
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            StaggeredCard(index = 1) {
                PlayerDeltaECard(
                    bestDeltaE = state.threshold.personalBestDeltaE,
                    onClick = { showDeltaESheet = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            StaggeredCard(index = 2) {
                LevelProgressBar(
                    fraction = levelProgressFraction(state.playerLevel, state.totalGems),
                    currentLevel = state.playerLevel,
                    totalGems = state.totalGems,
                    accentColor = state.playerLevel.levelColor,
                    onClick = { showLevelsSheet = true },
                    alreadyPlayed = alreadyPlayed,
                    modifier = Modifier.padding(horizontal = HuezooSpacing.xs),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.sm))

            StaggeredCard(index = 3) {
                ThresholdHeroCard(
                    data = state.threshold,
                    isPaid = state.isPaid,
                    onEnterGame = onThresholdTap,
                    onWatchAd = { showPaywallSheet = true },
                )
            }

            if (state.threshold.isBlocked && !state.isPaid) {
                Spacer(Modifier.height(HuezooSpacing.md))
                UpgradeCta(onClick = onUpgradeTap)
                Spacer(Modifier.height(HuezooSpacing.xs))
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            StaggeredCard(index = 4) {
                DailyCompactCard(
                    data = state.daily,
                    challengeName = challengeName,
                    streak = state.streak,
                    onClick = onDailyTap,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.md))

            StaggeredCard(index = 5) {
                LeaderboardCompactCard(
                    personalBestDeltaE = state.threshold.personalBestDeltaE,
                    onClick = {
                        if (state.isPaid) onLeaderboardTap() else onUpgradeTap()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            DeltaEInfoCard()

            // Extra 56 dp when free-tier banner is visible so cards scroll clear of the overlay.
            val bannerReserve = if (state.isPaid) 0.dp else 56.dp
            Spacer(Modifier.height(HuezooSpacing.xxl + bannerReserve).navigationBarsPadding())
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSheetOverlays(
    showPaywallSheet: Boolean,
    showLevelsSheet: Boolean,
    showDeltaESheet: Boolean,
    totalGems: Int,
    deltaE: Float?,
    onPaywallDismiss: () -> Unit,
    onPaywallUpgrade: () -> Unit,
    onLevelsDismiss: () -> Unit,
    onDeltaEDismiss: () -> Unit,
) {
    if (showPaywallSheet) {
        HuezooBottomSheet(onDismissRequest = onPaywallDismiss) {
            PaywallSheet(
                onWatchAd = {},
                onUnlock = onPaywallUpgrade,
                onDismiss = onPaywallDismiss,
            )
        }
    }
    if (showLevelsSheet) {
        LevelsProgressSheet(
            currentGems = totalGems,
            onDismiss = onLevelsDismiss,
        )
    }
    if (showDeltaESheet) {
        DeltaEWorldRankSheet(
            deltaE = deltaE,
            onDismiss = onDeltaEDismiss,
        )
    }
}

private fun levelProgressFraction(level: PlayerLevel, totalGems: Int): Float {
    val next = PlayerLevel.entries.getOrNull(level.ordinal + 1) ?: return 1f
    val range = (next.minGems - level.minGems).toFloat()
    val progress = (totalGems - level.minGems).toFloat()
    return (progress / range).coerceIn(0f, 1f)
}

// ── Previews ─────────────────────────────────────────────────────────────────

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun HomeReadyPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = HomeUiState.Ready(
                threshold = ThresholdCardData(
                    personalBestDeltaE = 1.4f,
                    attemptsRemaining = 3,
                    maxAttempts = 5,
                    isBlocked = false,
                ),
                daily = DailyCardData(isCompletedToday = false),
                isPaid = false,
                totalGems = 1250,
                playerLevel = PlayerLevel.Rookie,
                streak = 14,
            ),
            onThresholdTap = {},
            onDailyTap = {},
            onSettingsTap = {},
            onUpgradeTap = {},
            onLeaderboardTap = {},
            onTryGranted = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun HomeBlockedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = HomeUiState.Ready(
                threshold = ThresholdCardData(
                    personalBestDeltaE = 0.9f,
                    attemptsRemaining = 0,
                    maxAttempts = 5,
                    isBlocked = true,
                ),
                daily = DailyCardData(isCompletedToday = true, personalBestRounds = 5),
                isPaid = true,
                totalGems = 2450,
                playerLevel = PlayerLevel.Sharp,
                streak = 7,
            ),
            onThresholdTap = {},
            onDailyTap = {},
            onSettingsTap = {},
            onUpgradeTap = {},
            onLeaderboardTap = {},
            onTryGranted = {},
        )
    }
}
