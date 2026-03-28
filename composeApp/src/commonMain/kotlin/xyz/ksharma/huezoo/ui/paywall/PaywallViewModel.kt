package xyz.ksharma.huezoo.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.platform.ads.AdResult
import xyz.ksharma.huezoo.platform.ads.RewardedAdClient

private const val GEM_COST_PER_BONUS_TRY = 15
private const val BONUS_TRIES_PER_AD = 1
private const val BONUS_TRIES_PER_GEM_SPEND = 1

data class PaywallUiState(
    val gemBalance: Int = 0,
    val adReady: Boolean = false,
    val isLoadingAd: Boolean = false,
    val isSpendingGems: Boolean = false,
    val error: String? = null,
)

class PaywallViewModel(
    private val settingsRepository: SettingsRepository,
    private val rewardedAdClient: RewardedAdClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val gems = settingsRepository.getGems()
            _uiState.value = _uiState.value.copy(gemBalance = gems)
            loadAd()
        }
    }

    fun onWatchAd() {
        if (!rewardedAdClient.isReady) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAd = true, error = null)
            when (val result = rewardedAdClient.show()) {
                AdResult.Rewarded -> {
                    settingsRepository.addBonusTries(BONUS_TRIES_PER_AD)
                    _uiState.value = _uiState.value.copy(isLoadingAd = false, adReady = false)
                    loadAd()
                }
                AdResult.Dismissed -> {
                    _uiState.value = _uiState.value.copy(isLoadingAd = false)
                }
                is AdResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAd = false,
                        error = result.message,
                    )
                }
            }
        }
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

    private fun loadAd() {
        viewModelScope.launch {
            rewardedAdClient.load()
            _uiState.value = _uiState.value.copy(adReady = rewardedAdClient.isReady)
        }
    }

    companion object {
        const val GEM_COST = GEM_COST_PER_BONUS_TRY
    }
}
