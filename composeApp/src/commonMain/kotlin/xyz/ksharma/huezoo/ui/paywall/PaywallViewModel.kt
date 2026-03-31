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

    /**
     * True while [onRewardEarned] has fired but its coroutine hasn't yet committed the DB write.
     * [onAdDismissed] must NOT remove the [RewardedAd] composable during this window — doing so
     * disposes the composable's DisposableEffect, which unregisters the reward callback from the
     * ad library before [onRewardEarned] can complete, silently losing the earned try.
     */
    private var adRewardPending = false

    init {
        safeLaunch {
            _uiState.value = _uiState.value.copy(gemBalance = settingsRepository.getGems())
        }
    }

    fun onWatchAd() {
        adRewardPending = false
        _uiState.value = _uiState.value.copy(showRewardedAd = true, error = null)
    }

    fun onRewardEarned() {
        adRewardPending = true
        safeLaunch {
            settingsRepository.addBonusTries(BONUS_TRIES_PER_AD)
            adRewardPending = false
            _uiState.value = _uiState.value.copy(
                showRewardedAd = false,
                tryGrantedCount = _uiState.value.tryGrantedCount + 1,
            )
        }
    }

    fun onAdDismissed() {
        // If a reward DB write is in-flight, let onRewardEarned clear showRewardedAd once done.
        // Clearing it here would dispose RewardedAd and unregister the reward callback too early.
        if (!adRewardPending) {
            _uiState.value = _uiState.value.copy(showRewardedAd = false)
        }
    }

    fun onSpendGems() {
        val balance = _uiState.value.gemBalance
        if (balance < GEM_COST_PER_BONUS_TRY || _uiState.value.isSpendingGems) return
        safeLaunch {
            _uiState.value = _uiState.value.copy(isSpendingGems = true, error = null)

            val deductResult = runCatching { settingsRepository.addGems(-GEM_COST_PER_BONUS_TRY) }
            if (deductResult.isFailure) {
                _uiState.value = _uiState.value.copy(isSpendingGems = false)
                return@safeLaunch
            }

            val grantResult = runCatching { settingsRepository.addBonusTries(BONUS_TRIES_PER_GEM_SPEND) }
            if (grantResult.isFailure) {
                // Refund gems — best-effort, ignore secondary failure.
                runCatching { settingsRepository.addGems(GEM_COST_PER_BONUS_TRY) }
                val revertedBalance = runCatching { settingsRepository.getGems() }.getOrDefault(balance)
                _uiState.value = _uiState.value.copy(isSpendingGems = false, gemBalance = revertedBalance)
                return@safeLaunch
            }

            val newBalance = settingsRepository.getGems()
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
