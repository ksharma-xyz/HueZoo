package xyz.ksharma.huezoo.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.result.state.ResultUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ResultViewModel(
    private val navResult: Result,
    private val thresholdRepository: ThresholdRepository,
    private val dailyRepository: DailyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val best = when (navResult.gameId) {
                GameId.THRESHOLD -> thresholdRepository.getPersonalBest()
                GameId.DAILY -> dailyRepository.getPersonalBest()
                else -> null
            }

            // UX.6.1: For Threshold only, check whether the player has attempts remaining.
            // Daily is once-per-day so Play Again never applies.
            val canPlayAgain = if (navResult.gameId == GameId.THRESHOLD) {
                when (val status = thresholdRepository.getAttemptStatus(Clock.System.now())) {
                    is AttemptStatus.Available -> status.maxAttempts - status.attemptsUsed > 0
                    is AttemptStatus.Exhausted -> false
                }
            } else {
                false
            }

            _uiState.value = ResultUiState.Ready(
                gameId = navResult.gameId,
                deltaE = navResult.deltaE,
                roundsSurvived = navResult.roundsSurvived,
                score = navResult.score,
                isNewPersonalBest = best?.bestScore?.let { navResult.score > it } ?: true,
                personalBestDeltaE = best?.bestDeltaE,
                canPlayAgain = canPlayAgain,
            )
        }
    }
}
