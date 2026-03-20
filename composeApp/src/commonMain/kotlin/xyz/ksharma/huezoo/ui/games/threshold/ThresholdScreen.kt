package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.SkewedStatChip
import xyz.ksharma.huezoo.ui.components.SwatchBlock
import xyz.ksharma.huezoo.ui.components.SwatchBlockSize
import xyz.ksharma.huezoo.ui.components.SwatchBlockState
import xyz.ksharma.huezoo.ui.games.threshold.state.RoundPhase
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdNavEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

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
                is ThresholdUiState.Blocked -> BlockedContent(state = state)
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

        // Instruction title
        Text(
            text = "IDENTIFY THE OUTLIER",
            style = MaterialTheme.typography.titleLarge,
            color = HuezooColors.AccentCyan,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(HuezooSpacing.md))

        // Delta E chip — shows current sensitivity as a SkewedStatChip
        SkewedStatChip(
            label = "CURRENT ΔE",
            value = state.deltaE.fmt(),
            accentColor = HuezooColors.AccentCyan,
        )

        Spacer(Modifier.height(HuezooSpacing.xl))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val swatchCount = state.swatches.size
            val spacing = HuezooSpacing.md
            val adaptiveSize = ((maxWidth - spacing * (swatchCount - 1)) / swatchCount)
                .coerceAtMost(SwatchBlockSize.Medium.sizeDp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.swatches.forEachIndexed { index, swatch ->
                    SwatchBlock(
                        color = swatch.color,
                        state = swatch.displayState.toSwatchBlockState(),
                        sizeDp = adaptiveSize,
                        onClick = { onSwatchTap(index) },
                    )
                }
            }
        }

        Spacer(Modifier.height(HuezooSpacing.lg))

        // During Wrong phase: sting copy appears below instruction
        AnimatedVisibility(
            visible = state.roundPhase == RoundPhase.Wrong && state.stingCopy != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
        ) {
            Text(
                text = state.stingCopy.orEmpty(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = HuezooColors.AccentMagenta,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BlockedContent(
    state: ThresholdUiState.Blocked,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No tries left",
            style = MaterialTheme.typography.headlineMedium,
            color = HuezooColors.TextPrimary,
        )
        Spacer(Modifier.height(HuezooSpacing.sm))
        Text(
            text = "${state.attemptsUsed}/${state.maxAttempts} used this window",
            style = MaterialTheme.typography.bodyMedium,
            color = HuezooColors.TextSecondary,
        )
    }
}

private fun Float.fmt(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

private fun SwatchDisplayState.toSwatchBlockState(): SwatchBlockState = when (this) {
    SwatchDisplayState.Default -> SwatchBlockState.Default
    SwatchDisplayState.Correct -> SwatchBlockState.Correct
    SwatchDisplayState.Wrong -> SwatchBlockState.Wrong
    SwatchDisplayState.Revealed -> SwatchBlockState.Revealed
}

// ── Previews ─────────────────────────────────────────────────────────────────

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ThresholdPlayingPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        PlayingContent(
            state = ThresholdUiState.Playing(
                swatches = listOf(
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF4CAF50)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF4CAF50)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF66BB6A)),
                ),
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
private fun ThresholdBlockedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        BlockedContent(
            state = ThresholdUiState.Blocked(
                nextResetAt = kotlinx.datetime.Clock.System.now(),
                attemptsUsed = 5,
                maxAttempts = 5,
            ),
        )
    }
}
