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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.GameCard
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.HomeUiEvent
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

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

    GameCard(
        title = "The Threshold",
        subtitle = "How sharp are your eyes?",
        identityColor = HuezooColors.GameThreshold,
        onClick = onClick,
        enabled = !data.isBlocked,
        triesText = triesText,
        personalBest = personalBest,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DailyCard(
    data: DailyCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeText = if (data.isCompletedToday) "Done" else null
    val personalBest = data.todayScore?.let { "Score: ${it.toInt()}" }

    GameCard(
        title = "Daily Challenge",
        subtitle = "Same puzzle for everyone today",
        identityColor = HuezooColors.GameDaily,
        onClick = onClick,
        enabled = !data.isCompletedToday,
        badgeText = badgeText,
        personalBest = personalBest,
        modifier = modifier.fillMaxWidth(),
    )
}
