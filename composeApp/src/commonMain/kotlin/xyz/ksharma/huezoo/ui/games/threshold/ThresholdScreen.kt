package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import huezoo.composeapp.generated.resources.ic_chevron_left
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.DeltaEBadge
import xyz.ksharma.huezoo.ui.components.HuezooIconButton
import xyz.ksharma.huezoo.ui.components.HuezooIconButtonVariant
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

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is ThresholdNavEvent.NavigateToResult -> onResult(event.result)
                ThresholdNavEvent.NavigateBack -> onBack()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HuezooColors.Background),
    ) {
        when (val state = uiState) {
            ThresholdUiState.Loading -> Unit
            is ThresholdUiState.Blocked -> BlockedContent(state = state, onBack = onBack)
            is ThresholdUiState.Playing -> PlayingContent(
                state = state,
                onSwatchTap = { index -> viewModel.onUiEvent(ThresholdUiEvent.SwatchTapped(index)) },
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun PlayingContent(
    state: ThresholdUiState.Playing,
    onSwatchTap: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HuezooIconButton(
                variant = HuezooIconButtonVariant.Back,
                icon = painterResource(Res.drawable.ic_chevron_left),
                contentDescription = "Back",
                onClick = onBack,
            )
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HuezooIconButton(
            variant = HuezooIconButtonVariant.Back,
            icon = painterResource(Res.drawable.ic_chevron_left),
            contentDescription = "Back",
            onClick = onBack,
        )
        Spacer(Modifier.height(HuezooSpacing.xl))
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
