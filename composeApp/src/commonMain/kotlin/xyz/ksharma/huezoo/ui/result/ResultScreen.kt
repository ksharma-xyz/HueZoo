package xyz.ksharma.huezoo.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.ResultCard
import xyz.ksharma.huezoo.ui.result.state.ResultUiState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

@Composable
fun ResultScreen(
    result: Result,
    onLeaderboard: () -> Unit,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultViewModel = koinViewModel(parameters = { parametersOf(result) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HuezooColors.Background),
    ) {
        when (val state = uiState) {
            ResultUiState.Loading -> Unit
            is ResultUiState.Ready -> ReadyContent(
                state = state,
                onPlayAgain = onPlayAgain,
                onLeaderboard = onLeaderboard,
            )
        }
    }
}

@Composable
private fun ReadyContent(
    state: ResultUiState.Ready,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val identityColor = when (state.gameId) {
        GameId.DAILY -> HuezooColors.GameDaily
        else -> HuezooColors.GameThreshold
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
    ) {
        Spacer(Modifier.height(HuezooSpacing.lg))

        ResultCard(
            gameTitle = when (state.gameId) {
                GameId.DAILY -> "Daily Challenge"
                else -> "The Threshold"
            },
            deltaE = state.deltaE,
            score = state.score,
            roundsSurvived = state.roundsSurvived,
            identityColor = identityColor,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        HuezooButton(
            text = "Play Again",
            onClick = onPlayAgain,
            variant = HuezooButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        HuezooButton(
            text = "Leaderboard",
            onClick = onLeaderboard,
            variant = HuezooButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
