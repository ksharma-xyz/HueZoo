package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import xyz.ksharma.huezoo.ui.theme.HeartLife
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.DeltaEBadge
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.RadialSwatchLayout
import xyz.ksharma.huezoo.ui.components.ThresholdHelpSheet
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdNavEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
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
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        ThresholdHelpSheet(onDismiss = { showHelp = false })
    }

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
                onHelpClick = { showHelp = true },
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
        Spacer(Modifier.height(HuezooSpacing.lg))

        val accent = LocalPlayerAccentColor.current
        // ── Lives hearts — bottom of screen ──────────────────────────────────
        // Label left, hearts tightly clustered right.
        // Solid heart = life remaining, outline = life lost.
        // Color matches the player accent (same as title, back button).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = HuezooSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "IDENTIFY  THE  OUTLIER",
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.xs)) {
                repeat(state.maxAttempts) { index ->
                    val isRemaining = index < state.attemptsRemaining
                    val fillColor by animateColorAsState(
                        targetValue = if (isRemaining) accent else accent.copy(alpha = 0f),
                        animationSpec = tween(300),
                        label = "lifeHeart_$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(HEART_SIZE)
                            .border(
                                width = 1.5.dp,
                                color = accent.copy(alpha = 0.4f),
                                shape = HeartLife,
                            )
                            .background(fillColor, HeartLife),
                    )
                }
            }
        }

        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── ΔE hero — full centre focus, no competing elements ───────────────
        DeltaEBadge(deltaE = state.deltaE)

        Spacer(Modifier.height(HuezooSpacing.xs))

        // SESSION BEST — fades in after first correct tap, stays for the session
        val bestAlpha by animateFloatAsState(
            targetValue = if (state.sessionBestDeltaE != null) 1f else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "bestDeltaEAlpha",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer { alpha = bestAlpha },
        ) {
            HuezooLabelSmall(text = "SESSION BEST", color = HuezooColors.TextSecondary)
            HuezooLabelSmall(
                text = state.sessionBestDeltaE?.fmt() ?: "--",
                color = HuezooColors.AccentGreen,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.lg))

        // ── Fixed-height feedback slot ────────────────────────────────────────
        // Rules:
        //   • Box height is ALWAYS exactly FEEDBACK_SLOT_HEIGHT — never grows, never shrinks.
        //   • Both Text nodes live in the layout tree permanently (no AnimatedVisibility /
        //     AnimatedContent) so the Box never re-measures due to children entering or leaving.
        //   • Visibility is controlled by graphicsLayer { alpha } which is a pure render pass —
        //     it has zero effect on layout or measurement.
        //   • maxLines = 1 on the sting copy stops long strings from ever requesting extra height.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FEEDBACK_SLOT_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            // Correct feedback (green) ─────────────────────────────────────────
            val correctAlpha by animateFloatAsState(
                targetValue = if (state.roundPhase == RoundPhase.Correct) 1f else 0f,
                animationSpec = tween(durationMillis = 160),
                label = "feedbackCorrectAlpha",
            )
            Text(
                text = "↓ ΔE ${state.deltaE.fmt()} — SHARPER",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = HuezooColors.AccentGreen,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = correctAlpha },
            )

            // Wrong feedback / sting (magenta) ────────────────────────────────
            val wrongAlpha by animateFloatAsState(
                targetValue = if (state.roundPhase == RoundPhase.Wrong) 1f else 0f,
                animationSpec = tween(durationMillis = 160),
                label = "feedbackWrongAlpha",
            )
            Text(
                text = state.stingCopy.orEmpty(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = HuezooColors.AccentMagenta,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = wrongAlpha },
            )
        }

        // ── Radial swatch layout — takes all remaining vertical space ─────────
        RadialSwatchLayout(
            swatches = state.swatches,
            roundPhase = state.roundPhase,
            // roundGeneration increments on EVERY emitRound() call (correct + wrong-reset),
            // so the unfold animation always triggers — unlike state.round which only
            // increments on correct taps.
            roundKey = state.roundGeneration,
            layoutStyle = state.layoutStyle,
            onSwatchTap = onSwatchTap,
            modifier = Modifier.weight(1f),
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

/** Size of each heart in the lives indicator. */
private val HEART_SIZE = 14.dp

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
                tap = 3,
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
                tap = 3,
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
                tap = 5,
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
