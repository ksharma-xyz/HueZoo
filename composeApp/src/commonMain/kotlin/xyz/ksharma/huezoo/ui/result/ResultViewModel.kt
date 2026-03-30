package xyz.ksharma.huezoo.ui.result

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import xyz.ksharma.huezoo.ui.util.safeLaunch
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.game.SessionResultCache
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.SessionResult
import xyz.ksharma.huezoo.ui.result.state.ResultUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ResultViewModel(
    private val sessionResultCache: SessionResultCache,
    private val thresholdRepository: ThresholdRepository,
    private val dailyRepository: DailyRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        // Collect reactively so "play again" sessions trigger a fresh load automatically,
        // even when this ViewModel instance is reused by the DI container.
        safeLaunch {
            sessionResultCache.result.filterNotNull().collect { result ->
                load(result)
            }
        }
    }

    private suspend fun load(sessionResult: SessionResult) {
        println(
            "[DEBUG_RESULT] load() deltaE=${sessionResult.deltaE} gameId=${sessionResult.gameId} rounds=${sessionResult.roundsSurvived}/${sessionResult.totalRounds}",
        )
        val best = when (sessionResult.gameId) {
            GameId.THRESHOLD -> thresholdRepository.getPersonalBest()
            GameId.DAILY -> dailyRepository.getPersonalBest()
            else -> null
        }

        val canPlayAgain = if (sessionResult.gameId == GameId.THRESHOLD) {
            when (val status = thresholdRepository.getAttemptStatus(Clock.System.now())) {
                is AttemptStatus.Available -> status.maxAttempts - status.attemptsUsed > 0
                is AttemptStatus.Exhausted -> false
            }
        } else {
            false
        }
        val isPaid = settingsRepository.isPaid()

        val isNewPersonalBest = when (sessionResult.gameId) {
            GameId.THRESHOLD -> best?.bestDeltaE?.let {
                kotlin.math.abs(it - sessionResult.deltaE) < 0.005f
            } ?: true
            GameId.DAILY -> best?.bestRounds?.let { sessionResult.roundsSurvived >= it } ?: true
            else -> false
        }

        println("[DEBUG_RESULT] dbBest=${best?.bestDeltaE} isNewPersonalBest=$isNewPersonalBest")
        _uiState.value = ResultUiState.Ready(
            gameId = sessionResult.gameId,
            deltaE = sessionResult.deltaE,
            roundsSurvived = sessionResult.roundsSurvived,
            correctRounds = sessionResult.correctRounds,
            totalRounds = sessionResult.totalRounds,
            isNewPersonalBest = isNewPersonalBest,
            personalBestDeltaE = best?.bestDeltaE,
            gemsEarned = sessionResult.gemsEarned,
            gemBreakdown = sessionResult.gemBreakdown,
            canPlayAgain = canPlayAgain,
            isPaid = isPaid,
            levelUpTo = sessionResult.levelUpTo,
        )
    }
}
