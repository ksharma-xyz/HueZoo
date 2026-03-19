package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import huezoo.composeapp.generated.resources.Res
import huezoo.composeapp.generated.resources.ic_gem
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.DeltaEBadge
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.SwatchBlock
import xyz.ksharma.huezoo.ui.components.SwatchBlockSize
import xyz.ksharma.huezoo.ui.components.SwatchBlockState
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
    val gemIcon = painterResource(Res.drawable.ic_gem)

    LaunchedEffect(Unit) {
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
                currencyAmount = 0,
                gemIcon = gemIcon,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.attemptsRemaining} tries left",
                style = MaterialTheme.typography.labelMedium,
                color = HuezooColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.xl))
        DeltaEBadge(deltaE = state.deltaE)
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

private fun SwatchDisplayState.toSwatchBlockState(): SwatchBlockState = when (this) {
    SwatchDisplayState.Default -> SwatchBlockState.Default
    SwatchDisplayState.Correct -> SwatchBlockState.Correct
    SwatchDisplayState.Wrong -> SwatchBlockState.Wrong
    SwatchDisplayState.Revealed -> SwatchBlockState.Revealed
}
