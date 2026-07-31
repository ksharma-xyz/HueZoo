package xyz.ksharma.huezoo.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.debug.DebugFlags
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.billing.BillingClient
import xyz.ksharma.huezoo.platform.billing.PRODUCT_UNLIMITED
import xyz.ksharma.huezoo.ui.settings.state.SettingsUiEvent
import xyz.ksharma.huezoo.ui.settings.state.SettingsUiState
import xyz.ksharma.huezoo.ui.util.safeLaunch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val platformOps: PlatformOps,
    private val billingClient: BillingClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onUiEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.TogglePaid -> togglePaid()
            SettingsUiEvent.ToggleForceStreakCelebration -> toggleForceStreakCelebration()
            SettingsUiEvent.ToggleHideAds -> toggleHideAds()
            SettingsUiEvent.ResetAllTapped -> _uiState.update { it.copy(showResetConfirm = true) }
            SettingsUiEvent.ResetAllDismissed -> _uiState.update { it.copy(showResetConfirm = false) }
            SettingsUiEvent.ResetAllConfirmed -> resetAll()
            is SettingsUiEvent.NameInputChanged -> _uiState.update { it.copy(nameInput = event.value) }
            SettingsUiEvent.SaveNameTapped -> saveName()
            SettingsUiEvent.RestorePurchasesTapped -> restorePurchases()
        }
    }

    private fun load() {
        safeLaunch {
            val isPaid = settingsRepository.isPaid()
            val gems = settingsRepository.getGems()
            val userName = settingsRepository.getUserName()
            val isDebug = platformOps.isDebugBuild
            val appVersion = if (isDebug) "${platformOps.appVersion} dev" else platformOps.appVersion
            _uiState.update {
                it.copy(
                    isPaid = isPaid,
                    gems = gems,
                    isDebugBuild = isDebug,
                    userName = userName,
                    nameInput = userName ?: "",
                    forceStreakCelebration = DebugFlags.forceStreakCelebration,
                    hideAds = DebugFlags.hideAds,
                    appVersion = appVersion,
                )
            }
        }
    }

    private fun togglePaid() {
        safeLaunch {
            val newPaid = !_uiState.value.isPaid
            settingsRepository.setPaid(newPaid)
            _uiState.update { it.copy(isPaid = newPaid) }
        }
    }

    private fun saveName() {
        val name = _uiState.value.nameInput.trim()
        if (name.isBlank()) return
        safeLaunch {
            settingsRepository.setUserName(name)
            _uiState.update { it.copy(userName = name) }
        }
    }

    private fun toggleForceStreakCelebration() {
        val newValue = !DebugFlags.forceStreakCelebration
        DebugFlags.forceStreakCelebration = newValue
        _uiState.update { it.copy(forceStreakCelebration = newValue) }
    }

    private fun toggleHideAds() {
        val newValue = !DebugFlags.hideAds
        DebugFlags.hideAds = newValue
        _uiState.update { it.copy(hideAds = newValue) }
    }

    private fun restorePurchases() {
        if (_uiState.value.isRestoring) return
        safeLaunch {
            _uiState.update { it.copy(isRestoring = true, restoreMessage = null) }
            val owned = billingClient.isOwned(PRODUCT_UNLIMITED)
            if (owned) {
                settingsRepository.setPaid(true)
                _uiState.update { it.copy(isRestoring = false, isPaid = true, restoreMessage = "Purchase restored!") }
            } else {
                _uiState.update { it.copy(isRestoring = false, restoreMessage = "No previous purchase found.") }
            }
        }
    }

    private fun resetAll() {
        safeLaunch {
            settingsRepository.resetAll()
            load()
            _uiState.update { it.copy(showResetConfirm = false) }
        }
    }
}
