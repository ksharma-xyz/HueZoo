package xyz.ksharma.huezoo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.HomeUiEvent
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HomeViewModel(
    private val thresholdRepository: ThresholdRepository,
    private val dailyRepository: DailyRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onUiEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.ScreenResumed -> load()
            HomeUiEvent.DebugResetTapped -> debugReset()
            else -> Unit // Navigation events handled directly in the screen composable.
        }
    }

    private fun debugReset() {
        viewModelScope.launch {
            settingsRepository.resetAll()
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

            val attemptStatus = thresholdRepository.getAttemptStatus(now)
            val thresholdBest = thresholdRepository.getPersonalBest()
            val dailyChallenge = dailyRepository.getChallenge(today)
            val isPaid = settingsRepository.isPaid()
            val totalGems = settingsRepository.getGems()

            val thresholdCard = when (attemptStatus) {
                is AttemptStatus.Available -> ThresholdCardData(
                    personalBestDeltaE = thresholdBest?.bestDeltaE,
                    attemptsRemaining = attemptStatus.maxAttempts - attemptStatus.attemptsUsed,
                    maxAttempts = attemptStatus.maxAttempts,
                    isBlocked = false,
                )
                is AttemptStatus.Exhausted -> ThresholdCardData(
                    personalBestDeltaE = thresholdBest?.bestDeltaE,
                    attemptsRemaining = 0,
                    maxAttempts = attemptStatus.maxAttempts,
                    isBlocked = true,
                )
            }

            val dailyCard = DailyCardData(
                isCompletedToday = dailyChallenge?.completed == true,
                todayScore = dailyChallenge?.score,
            )

            _uiState.value = HomeUiState.Ready(
                threshold = thresholdCard,
                daily = dailyCard,
                isPaid = isPaid,
                totalGems = totalGems,
            )
        }
    }
}
