package xyz.ksharma.huezoo.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import huezoo.composeapp.generated.resources.Res
import huezoo.composeapp.generated.resources.ic_gem
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
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
    val gemIcon = painterResource(Res.drawable.ic_gem)

    val identityColor = if (result.gameId == GameId.DAILY) {
        HuezooColors.GameDaily
    } else {
        HuezooColors.GameThreshold
    }

    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = identityColor,
        secondaryColor = HuezooColors.AccentCyan,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HuezooTopBar(
                onBackClick = onBack,
                currencyAmount = 0,
                gemIcon = gemIcon,
            )

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
    val isDaily = state.gameId == GameId.DAILY

    Column(
        modifier = modifier
            .fillMaxSize()
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

        // Daily is once-per-day — "Play Again" is misleading, show "Back to Home" instead.
        HuezooButton(
            text = if (isDaily) "Back to Home" else "Play Again",
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
