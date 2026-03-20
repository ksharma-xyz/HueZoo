package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.FlowerSwatchLayout
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.SkewedStatChip
import xyz.ksharma.huezoo.ui.games.threshold.state.RoundPhase
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdNavEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun ThresholdScreen(
    onResult: (Result) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThresholdViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onStart()
        viewModel.navEvent.collect { event ->
            when (event) {
                is ThresholdNavEvent.NavigateToResult -> onResult(event.result)
                ThresholdNavEvent.NavigateBack -> onBack()
            }
        }
    }

    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = HuezooColors.GameThreshold,
        secondaryColor = HuezooColors.AccentPurple,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HuezooTopBar(
                onBackClick = onBack,
                currencyAmount = (uiState as? ThresholdUiState.Playing)?.totalGems,
            )

            when (val state = uiState) {
                ThresholdUiState.Loading -> Unit
                is ThresholdUiState.Blocked -> BlockedContent(
                    state = state,
                    onBack = onBack,
                )
                is ThresholdUiState.Playing -> PlayingContent(
                    state = state,
                    onSwatchTap = { index ->
                        viewModel.onUiEvent(ThresholdUiEvent.SwatchTapped(index))
                    },
                )
            }
        }
    }
}

@Composable
private fun PlayingContent(
    state: ThresholdUiState.Playing,
    onSwatchTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            SkewedStatChip(
                label = "ROUND",
                value = state.round.toString(),
                accentColor = HuezooColors.GameThreshold,
            )
            SkewedStatChip(
                label = "TRIES",
                value = state.attemptsRemaining.toString(),
                accentColor = HuezooColors.AccentMagenta,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.xxl))

        // Instruction title — fades out during fold so the eye follows the petals closing
        AnimatedVisibility(
            visible = state.roundPhase != RoundPhase.FoldingOut,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
        ) {
            Text(
                text = "IDENTIFY THE OUTLIER",
                style = MaterialTheme.typography.titleLarge,
                color = HuezooColors.AccentCyan,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        // Delta E chip
        SkewedStatChip(
            label = "CURRENT ΔE",
            value = state.deltaE.fmt(),
            accentColor = HuezooColors.AccentCyan,
        )

        Spacer(Modifier.height(HuezooSpacing.lg))

        // ── Fixed-height feedback slot ────────────────────────────────────────
        // Height is always 28dp; content fades in/out inside it.
        // Nothing outside this Box shifts when the message changes.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FEEDBACK_SLOT_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            val feedbackText: String? = when (state.roundPhase) {
                RoundPhase.Correct -> "↓ ΔE ${state.deltaE.fmt()} — SHARPER"
                RoundPhase.Wrong -> state.stingCopy
                else -> null
            }
            val feedbackColor = when (state.roundPhase) {
                RoundPhase.Correct -> HuezooColors.AccentGreen
                else -> HuezooColors.AccentMagenta
            }
            AnimatedContent(
                targetState = feedbackText,
                transitionSpec = {
                    fadeIn(tween(140)) togetherWith fadeOut(tween(180))
                },
                label = "feedbackSlot",
            ) { text ->
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = feedbackColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── Flower swatch layout ──────────────────────────────────────────────
        FlowerSwatchLayout(
            swatches = state.swatches,
            roundPhase = state.roundPhase,
            roundKey = state.round,
            onSwatchTap = onSwatchTap,
        )
    }
}

// UX.3.2 — Styled blocked screen: live countdown + back button
@OptIn(ExperimentalTime::class)
@Composable
private fun BlockedContent(
    state: ThresholdUiState.Blocked,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdown by countdownUntil(state.nextResetAt)

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "OUT OF TRIES",
            style = MaterialTheme.typography.headlineMedium,
            color = HuezooColors.AccentMagenta,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HuezooSpacing.sm))
        Text(
            text = "${state.attemptsUsed}/${state.maxAttempts} used this window",
            style = MaterialTheme.typography.bodyMedium,
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HuezooSpacing.xl))
        if (countdown.isNotEmpty()) {
            Text(
                text = "Resets in $countdown",
                style = MaterialTheme.typography.titleMedium,
                color = HuezooColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))

        HuezooButton(
            text = "BACK TO HOME",
            onClick = onBack,
            variant = HuezooButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HuezooSpacing.md))
    }
}

/** Returns a live-updating "Xh Xm" string, ticking every 60 s. */
@OptIn(ExperimentalTime::class)
@Composable
private fun countdownUntil(until: Instant) = produceState(initialValue = "") {
    while (true) {
        val remaining = until - Clock.System.now()
        val totalSeconds = remaining.inWholeSeconds.coerceAtLeast(0)
        value = if (totalSeconds <= 0) {
            ""
        } else {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
        delay(60_000L)
    }
}

/** Fixed height reserved for the in-game feedback message. Never changes, so nothing shifts. */
private val FEEDBACK_SLOT_HEIGHT = 28.dp

private fun Float.fmt(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val PREVIEW_PURPLE = androidx.compose.ui.graphics.Color(0xFF4CAF50)
private val PREVIEW_ODD = androidx.compose.ui.graphics.Color(0xFF66BB6A)

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ThresholdPlayingPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        PlayingContent(
            state = ThresholdUiState.Playing(
                swatches = List(5) { xyz.ksharma.huezoo.ui.model.SwatchUiModel(PREVIEW_PURPLE) } +
                    listOf(xyz.ksharma.huezoo.ui.model.SwatchUiModel(PREVIEW_ODD)),
                deltaE = 2.4f,
                round = 3,
                attemptsRemaining = 4,
                roundPhase = RoundPhase.Idle,
                totalGems = 24,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ThresholdCorrectPhasePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        PlayingContent(
            state = ThresholdUiState.Playing(
                swatches = listOf(
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(
                        PREVIEW_PURPLE,
                        displayState = SwatchDisplayState.Correct,
                    ),
                ) + List(5) { xyz.ksharma.huezoo.ui.model.SwatchUiModel(PREVIEW_PURPLE) },
                deltaE = 2.4f,
                round = 3,
                attemptsRemaining = 4,
                roundPhase = RoundPhase.Correct,
                totalGems = 26,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ThresholdWrongPhasePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        PlayingContent(
            state = ThresholdUiState.Playing(
                swatches = listOf(
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(
                        androidx.compose.ui.graphics.Color(0xFFE91E63),
                        displayState = SwatchDisplayState.Wrong,
                    ),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(
                        androidx.compose.ui.graphics.Color(0xFFF06292),
                        displayState = SwatchDisplayState.Revealed,
                    ),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                ),
                deltaE = 1.8f,
                round = 5,
                attemptsRemaining = 2,
                roundPhase = RoundPhase.Wrong,
                stingCopy = "So close. And yet.",
                totalGems = 32,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
@kotlin.OptIn(kotlin.time.ExperimentalTime::class)
private fun ThresholdBlockedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        BlockedContent(
            state = ThresholdUiState.Blocked(
                nextResetAt = kotlin.time.Clock.System.now()
                    .plus(kotlin.time.Duration.parse("2h30m")),
                attemptsUsed = 5,
                maxAttempts = 5,
            ),
            onBack = {},
        )
    }
}

