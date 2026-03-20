package xyz.ksharma.huezoo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.GameCard
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.HomeUiEvent
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val STAGGER_DELAY_MS = 80L
private const val CARD_ANIM_DURATION_MS = 300
private const val SLIDE_FRACTION = 5

@Composable
fun HomeScreen(
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platformOps: PlatformOps = koinInject()

    // Re-load every time HomeScreen becomes the active top entry (e.g. returning from a game).
    LifecycleResumeEffect(Unit) {
        viewModel.onUiEvent(HomeUiEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    AmbientGlowBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            HuezooTopBar(
                currencyAmount = (uiState as? HomeUiState.Ready)?.totalGems,
            )

            when (val state = uiState) {
                HomeUiState.Loading -> Unit
                is HomeUiState.Ready -> ReadyContent(
                    state = state,
                    onThresholdTap = { onNavigate(ThresholdGame) },
                    onDailyTap = { onNavigate(DailyGame) },
                    showDebugReset = platformOps.isDebugBuild,
                    onDebugReset = { viewModel.onUiEvent(HomeUiEvent.DebugResetTapped) },
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    onThresholdTap: () -> Unit,
    onDailyTap: () -> Unit,
    modifier: Modifier = Modifier,
    showDebugReset: Boolean = false,
    onDebugReset: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(HuezooSpacing.md),
    ) {
        Spacer(Modifier.height(HuezooSpacing.md))

        StaggeredCard(index = 0) {
            ThresholdCard(data = state.threshold, onClick = onThresholdTap)
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        StaggeredCard(index = 1) {
            DailyCard(data = state.daily, onClick = onDailyTap)
        }

        if (showDebugReset) {
            Spacer(Modifier.height(HuezooSpacing.xl))
            HuezooButton(
                text = "DEBUG: RESET ALL",
                onClick = onDebugReset,
                variant = HuezooButtonVariant.GhostDanger,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StaggeredCard(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * STAGGER_DELAY_MS)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(CARD_ANIM_DURATION_MS)) +
            slideInVertically(tween(CARD_ANIM_DURATION_MS)) { it / SLIDE_FRACTION },
    ) {
        content()
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ThresholdCard(
    data: ThresholdCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val triesText = when {
        data.isBlocked -> "No tries left"
        else -> "${data.attemptsRemaining}/${data.maxAttempts} tries remaining"
    }
    val personalBest = data.personalBestDeltaE?.let { de ->
        val rounded = (de * 10).toInt() / 10.0
        "Best: ΔE $rounded"
    }
    val countdown = data.nextResetAt?.let { countdownUntil(it, prefix = "Resets in ") }

    GameCard(
        title = "The Threshold",
        subtitle = "How sharp are your eyes?",
        identityColor = HuezooColors.GameThreshold,
        onClick = onClick,
        enabled = !data.isBlocked,
        triesText = triesText,
        personalBest = personalBest,
        countdownText = countdown,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun DailyCard(
    data: DailyCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeText = if (data.isCompletedToday) "Done" else null
    val personalBest = data.todayScore?.let { "Score: ${it.toInt()}" }
    val countdown = data.nextPuzzleAt?.let { countdownUntil(it, prefix = "Next puzzle in ") }

    GameCard(
        title = "Daily Challenge",
        subtitle = "Same puzzle for everyone today",
        identityColor = HuezooColors.GameDaily,
        onClick = onClick,
        enabled = !data.isCompletedToday,
        badgeText = badgeText,
        personalBest = personalBest,
        countdownText = countdown,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Returns a live-updating countdown string (e.g. "Resets in 2h 14m") that
 * ticks every minute until [until] is reached.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun countdownUntil(until: Instant, prefix: String): String {
    val text by produceState(initialValue = "") {
        while (true) {
            val remaining = until - Clock.System.now()
            val totalSeconds = remaining.inWholeSeconds.coerceAtLeast(0)
            value = if (totalSeconds <= 0) {
                ""
            } else {
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                if (hours > 0) "$prefix${hours}h ${minutes}m" else "$prefix${minutes}m"
            }
            delay(60_000L)
        }
    }
    return text
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
                daily = DailyCardData(
                    isCompletedToday = false,
                    todayScore = null,
                ),
                isPaid = false,
                totalGems = 128,
            ),
            onThresholdTap = {},
            onDailyTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun HomeBlockedAndCompletedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = HomeUiState.Ready(
                threshold = ThresholdCardData(
                    personalBestDeltaE = 0.9f,
                    attemptsRemaining = 0,
                    maxAttempts = 5,
                    isBlocked = true,
                ),
                daily = DailyCardData(
                    isCompletedToday = true,
                    todayScore = 740f,
                ),
                isPaid = true,
                totalGems = 2450,
            ),
            onThresholdTap = {},
            onDailyTap = {},
        )
    }
}
