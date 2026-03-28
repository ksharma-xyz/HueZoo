package xyz.ksharma.huezoo.ui.games.daily

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.platform.ads.BannerAd
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.DailyHelpSheet
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.RadialSwatchLayout
import xyz.ksharma.huezoo.ui.components.SkewedStatChip
import xyz.ksharma.huezoo.ui.games.daily.state.DailyNavEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiState
import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun DailyScreen(
    onResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPaid by viewModel.isPaid.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        DailyHelpSheet(onDismiss = { showHelp = false })
    }

    // Collect nav events. Fresh subscriber on config-change starts collecting future events.
    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                DailyNavEvent.NavigateToResult -> onResult()
                DailyNavEvent.NavigateBack -> onBack()
            }
        }
    }

    // Re-check today's challenge status only when already showing AlreadyPlayed
    // (e.g. day changed while app was backgrounded). Never resets active gameplay.
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    Box(modifier = modifier) {
        AmbientGlowBackground(
            modifier = Modifier.fillMaxSize(),
            primaryColor = HuezooColors.GameDaily,
            secondaryColor = LocalPlayerAccentColor.current,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HuezooTopBar(
                    onBackClick = onBack,
                    currencyAmount = null,
                    onHelpClick = { showHelp = true },
                )

                when (val state = uiState) {
                    DailyUiState.Loading -> Unit
                    is DailyUiState.AlreadyPlayed -> AlreadyPlayedContent(
                        state = state,
                        onBack = onBack,
                    )
                    is DailyUiState.Playing -> DailyPlayingContent(
                        state = state,
                        onSwatchTap = { index -> viewModel.onUiEvent(DailyUiEvent.SwatchTapped(index)) },
                    )
                }
            }
        }

        if (!isPaid && uiState !is DailyUiState.Loading) {
            BannerAd(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun DailyPlayingContent(
    state: DailyUiState.Playing,
    onSwatchTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // UX.4.2 — Date subtitle computed once per composition
    val dateSubtitle = remember {
        val tz = TimeZone.currentSystemDefault()
        val date = Clock.System.now().toLocalDateTime(tz).date
        val monthName = date.month.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
        "$monthName ${date.day} · Same for everyone"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkewedStatChip(
            label = "ROUND",
            value = "${state.round}/${state.totalRounds}",
            accentColor = HuezooColors.GameDaily,
        )

        Spacer(Modifier.height(HuezooSpacing.xxl))

        // Title — fades out during fold so layout stays stable (same technique as Threshold)
        val titleAlpha by animateFloatAsState(
            targetValue = if (state.roundPhase == RoundPhase.FoldingOut) 0f else 1f,
            animationSpec = tween(durationMillis = 150),
            label = "titleAlpha",
        )
        Text(
            text = "DAILY CHALLENGE",
            style = MaterialTheme.typography.titleLarge,
            color = LocalPlayerAccentColor.current,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = titleAlpha },
        )
        Spacer(Modifier.height(HuezooSpacing.xs))
        Text(
            text = dateSubtitle,
            style = MaterialTheme.typography.labelMedium,
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = titleAlpha },
        )

        Spacer(Modifier.height(HuezooSpacing.lg))

        // ── Fixed-height feedback slot (same technique as Threshold) ──────────
        // Both Text nodes are always in the tree; visibility via graphicsLayer alpha.
        // The fixed height means nothing below ever shifts.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FEEDBACK_SLOT_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            // Correct feedback (green)
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

            // Wrong feedback (magenta) — UX.4.3 last-round copy
            val wrongAlpha by animateFloatAsState(
                targetValue = if (state.roundPhase == RoundPhase.Wrong) 1f else 0f,
                animationSpec = tween(durationMillis = 160),
                label = "feedbackWrongAlpha",
            )
            Text(
                text = if (state.round == state.totalRounds) "Last one — make it count" else "Wrong one!",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = HuezooColors.AccentMagenta,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = wrongAlpha },
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── Radial swatch layout — same component as Threshold ────────────────
        RadialSwatchLayout(
            swatches = state.swatches,
            roundPhase = state.roundPhase,
            roundKey = state.roundGeneration,
            layoutStyle = state.layoutStyle,
            onSwatchTap = onSwatchTap,
        )
    }
}

// UX.3.1 — Styled already-played screen with score, live countdown, and back button
@OptIn(ExperimentalTime::class)
@Composable
private fun AlreadyPlayedContent(
    state: DailyUiState.AlreadyPlayed,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextPuzzleAt = remember {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
    }
    val countdown by countdownUntil(nextPuzzleAt)

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "ALREADY PLAYED",
            style = MaterialTheme.typography.headlineMedium,
            color = HuezooColors.GameDaily,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HuezooSpacing.xl))
        if (countdown.isNotEmpty()) {
            Text(
                text = "Next puzzle in $countdown",
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

/** Fixed height reserved for the in-game feedback message. Matches Threshold. */
private val FEEDBACK_SLOT_HEIGHT = 28.dp

private fun Float.fmt(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val PREVIEW_BASE = HuezooColors.AccentCyan
private val PREVIEW_ODD = HuezooColors.GameDaily

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun DailyPlayingPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        DailyPlayingContent(
            state = DailyUiState.Playing(
                swatches = List(5) { SwatchUiModel(PREVIEW_BASE) } +
                    listOf(SwatchUiModel(PREVIEW_ODD)),
                deltaE = 3.1f,
                round = 2,
                totalRounds = 6,
                roundPhase = RoundPhase.Idle,
                layoutStyle = SwatchLayoutStyle.Flower,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun DailyCorrectPhasePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        DailyPlayingContent(
            state = DailyUiState.Playing(
                swatches = listOf(SwatchUiModel(PREVIEW_ODD, displayState = SwatchDisplayState.Correct)) +
                    List(5) { SwatchUiModel(PREVIEW_BASE) },
                deltaE = 2.0f,
                round = 3,
                totalRounds = 6,
                roundPhase = RoundPhase.Correct,
                layoutStyle = SwatchLayoutStyle.HexRing,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun DailyLastRoundPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        DailyPlayingContent(
            state = DailyUiState.Playing(
                swatches = List(5) { SwatchUiModel(PREVIEW_BASE) } +
                    listOf(SwatchUiModel(PREVIEW_ODD)),
                deltaE = 0.7f,
                round = 6,
                totalRounds = 6,
                roundPhase = RoundPhase.Idle,
                layoutStyle = SwatchLayoutStyle.DiamondHalo,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
@kotlin.OptIn(kotlin.time.ExperimentalTime::class)
private fun DailyAlreadyPlayedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        AlreadyPlayedContent(
            state = DailyUiState.AlreadyPlayed,
            onBack = {},
        )
    }
}
