package xyz.ksharma.huezoo.ui.games.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
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
                is DailyUiState.AlreadyPlayed -> AlreadyPlayedContent(state = state)
                is DailyUiState.Playing -> DailyPlayingContent(
                    state = state,
                    onSwatchTap = { index -> viewModel.onUiEvent(DailyUiEvent.SwatchTapped(index)) },
                )
            }
        }
    }
}

@Composable
private fun DailyPlayingContent(
    state: DailyUiState.Playing,
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
                value = "${state.round}/${state.totalRounds}",
                accentColor = HuezooColors.GameDaily,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.xl))
        Text(
            text = "Daily Challenge",
            style = MaterialTheme.typography.headlineMedium,
            color = HuezooColors.TextPrimary,
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
        Text(
            text = "Tap the odd one out",
            style = MaterialTheme.typography.bodyMedium,
            color = HuezooColors.TextSecondary,
        )
    }
}

@Composable
private fun AlreadyPlayedContent(
    state: DailyUiState.AlreadyPlayed,
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
            text = "Already played today!",
            style = MaterialTheme.typography.headlineMedium,
            color = HuezooColors.TextPrimary,
        )
        Spacer(Modifier.height(HuezooSpacing.sm))
        Text(
            text = "Score: ${state.score.toInt()}",
            style = MaterialTheme.typography.bodyLarge,
            color = HuezooColors.TextSecondary,
        )
    }
}

private fun SwatchDisplayState.toSwatchBlockState(): SwatchBlockState = when (this) {
    SwatchDisplayState.Default -> SwatchBlockState.Default
    SwatchDisplayState.Correct -> SwatchBlockState.Correct
    SwatchDisplayState.Wrong -> SwatchBlockState.Wrong
    SwatchDisplayState.Revealed -> SwatchBlockState.Revealed
}
