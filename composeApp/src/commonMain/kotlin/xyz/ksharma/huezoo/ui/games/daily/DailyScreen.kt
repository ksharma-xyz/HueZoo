package xyz.ksharma.huezoo.ui.games.daily

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.SkewedStatChip
import xyz.ksharma.huezoo.ui.components.SwatchBlock
import xyz.ksharma.huezoo.ui.components.SwatchBlockSize
import xyz.ksharma.huezoo.ui.components.SwatchBlockState
import xyz.ksharma.huezoo.ui.games.daily.state.DailyNavEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiState
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun DailyScreen(
    onResult: (Result) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is DailyNavEvent.NavigateToResult -> onResult(event.result)
                DailyNavEvent.NavigateBack -> onBack()
            }
        }
    }

    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = HuezooColors.GameDaily,
        secondaryColor = HuezooColors.AccentCyan,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HuezooTopBar(
                onBackClick = onBack,
                currencyAmount = 0,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            SkewedStatChip(
                label = "ROUND",
                value = "${state.round}/${state.totalRounds}",
                accentColor = HuezooColors.GameDaily,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.xl))

        // UX.4.2 — Title + date subtitle
        Text(
            text = "DAILY CHALLENGE",
            style = MaterialTheme.typography.titleLarge,
            color = HuezooColors.AccentCyan,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HuezooSpacing.xs))
        Text(
            text = dateSubtitle,
            style = MaterialTheme.typography.labelMedium,
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(HuezooSpacing.xl))

        Row(
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.swatches.forEachIndexed { index, swatch ->
                SwatchBlock(
                    color = swatch.color,
                    state = swatch.displayState.toSwatchBlockState(),
                    size = SwatchBlockSize.Medium,
                    onClick = { onSwatchTap(index) },
                )
            }
        }

        Spacer(Modifier.height(HuezooSpacing.lg))

        // UX.4.3 — Last-round copy change
        Text(
            text = if (state.round == state.totalRounds) "Last one — make it count" else "Tap the odd one out",
            style = MaterialTheme.typography.bodyMedium,
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
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
        Spacer(Modifier.height(HuezooSpacing.sm))
        Text(
            text = "Today's score: ${state.score.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            color = HuezooColors.TextSecondary,
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

private fun SwatchDisplayState.toSwatchBlockState(): SwatchBlockState = when (this) {
    SwatchDisplayState.Default -> SwatchBlockState.Default
    SwatchDisplayState.Correct -> SwatchBlockState.Correct
    SwatchDisplayState.Wrong -> SwatchBlockState.Wrong
    SwatchDisplayState.Revealed -> SwatchBlockState.Revealed
}

// ── Previews ─────────────────────────────────────────────────────────────────

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
@kotlin.OptIn(kotlin.time.ExperimentalTime::class)
private fun DailyPlayingPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        DailyPlayingContent(
            state = DailyUiState.Playing(
                swatches = listOf(
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF1565C0)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF1565C0)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF1976D2)),
                ),
                deltaE = 3.1f,
                round = 2,
                totalRounds = 5,
                roundPhase = xyz.ksharma.huezoo.ui.games.daily.state.DailyRoundPhase.Idle,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
@kotlin.OptIn(kotlin.time.ExperimentalTime::class)
private fun DailyLastRoundPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        DailyPlayingContent(
            state = DailyUiState.Playing(
                swatches = listOf(
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF1565C0)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF1565C0)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFF1976D2)),
                ),
                deltaE = 1.4f,
                round = 5,
                totalRounds = 5,
                roundPhase = xyz.ksharma.huezoo.ui.games.daily.state.DailyRoundPhase.Idle,
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
            state = DailyUiState.AlreadyPlayed(score = 740f),
            onBack = {},
        )
    }
}
