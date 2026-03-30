package xyz.ksharma.huezoo.ui.paywall

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.ui.util.safeLaunch

private const val GEM_COST_PER_BONUS_TRY = 100
private const val BONUS_TRIES_PER_AD = 1
private const val BONUS_TRIES_PER_GEM_SPEND = 1

data class PaywallUiState(
    val gemBalance: Int = 0,
    val showRewardedAd: Boolean = false,
    val isSpendingGems: Boolean = false,
    /**
     * Incremented each time a bonus try is successfully granted (gems spent or ad watched).
     * PaywallSheet compares against the count it saw on open — dismisses only when count rises.
     * Counter-based to avoid instant-dismiss on re-open if Boolean stayed true in the ViewModel.
     */
    val tryGrantedCount: Int = 0,
    val error: String? = null,
)

class PaywallViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        safeLaunch {
            _uiState.value = _uiState.value.copy(gemBalance = settingsRepository.getGems())
        }
    }

    fun onWatchAd() {
        println("[DEBUG_PAYWALL] onWatchAd: triggering rewarded ad")
        _uiState.value = _uiState.value.copy(showRewardedAd = true, error = null)
    }

    fun onRewardEarned() {
        safeLaunch {
            settingsRepository.addBonusTries(BONUS_TRIES_PER_AD)
            println("[DEBUG_PAYWALL] onRewardEarned: bonus try granted, incrementing tryGrantedCount")
            _uiState.value = _uiState.value.copy(
                showRewardedAd = false,
                tryGrantedCount = _uiState.value.tryGrantedCount + 1,
            )
        }
    }

    fun onAdDismissed() {
        _uiState.value = _uiState.value.copy(showRewardedAd = false)
    }

    fun onSpendGems() {
        val balance = _uiState.value.gemBalance
        if (balance < GEM_COST_PER_BONUS_TRY || _uiState.value.isSpendingGems) return
        safeLaunch {
            _uiState.value = _uiState.value.copy(isSpendingGems = true, error = null)
            settingsRepository.addGems(-GEM_COST_PER_BONUS_TRY)
            settingsRepository.addBonusTries(BONUS_TRIES_PER_GEM_SPEND)
            val newBalance = settingsRepository.getGems()
            println("[DEBUG_PAYWALL] onSpendGems: gems spent, incrementing tryGrantedCount")
            _uiState.value = _uiState.value.copy(
                isSpendingGems = false,
                gemBalance = newBalance,
                tryGrantedCount = _uiState.value.tryGrantedCount + 1,
            )
        }
    }

    companion object {
        const val GEM_COST = GEM_COST_PER_BONUS_TRY
    }
}
