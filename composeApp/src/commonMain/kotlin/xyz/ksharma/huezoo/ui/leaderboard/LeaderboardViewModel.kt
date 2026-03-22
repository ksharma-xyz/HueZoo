package xyz.ksharma.huezoo.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.data.repository.ThresholdRepository

class LeaderboardViewModel(
    private val thresholdRepository: ThresholdRepository,
) : ViewModel() {

    data class State(
        val personalBestDeltaE: Float? = null,
        val loading: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val best = thresholdRepository.getPersonalBest()
            _state.value = State(personalBestDeltaE = best?.bestDeltaE, loading = false)
        }
    }
}
