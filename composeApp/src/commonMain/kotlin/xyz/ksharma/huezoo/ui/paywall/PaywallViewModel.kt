package xyz.ksharma.huezoo.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.data.repository.SettingsRepository

private const val GEM_COST_PER_BONUS_TRY = 15
private const val BONUS_TRIES_PER_AD = 1
private const val BONUS_TRIES_PER_GEM_SPEND = 1

data class PaywallUiState(
    val gemBalance: Int = 0,
    val showRewardedAd: Boolean = false,
    val isSpendingGems: Boolean = false,
    val error: String? = null,
)

class PaywallViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(gemBalance = settingsRepository.getGems())
        }
    }

    fun onWatchAd() {
        _uiState.value = _uiState.value.copy(showRewardedAd = true, error = null)
    }

    fun onRewardEarned() {
        viewModelScope.launch {
            settingsRepository.addBonusTries(BONUS_TRIES_PER_AD)
            _uiState.value = _uiState.value.copy(showRewardedAd = false)
        }
    }

    fun onAdDismissed() {
        _uiState.value = _uiState.value.copy(showRewardedAd = false)
    }

    fun onSpendGems() {
        val balance = _uiState.value.gemBalance
        if (balance < GEM_COST_PER_BONUS_TRY || _uiState.value.isSpendingGems) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpendingGems = true, error = null)
            settingsRepository.addGems(-GEM_COST_PER_BONUS_TRY)
            settingsRepository.addBonusTries(BONUS_TRIES_PER_GEM_SPEND)
            val newBalance = settingsRepository.getGems()
            _uiState.value = _uiState.value.copy(isSpendingGems = false, gemBalance = newBalance)
        }
    }

    companion object {
        const val GEM_COST = GEM_COST_PER_BONUS_TRY
    }
}
