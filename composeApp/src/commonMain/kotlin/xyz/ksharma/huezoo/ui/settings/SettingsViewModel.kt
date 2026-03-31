package xyz.ksharma.huezoo.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.debug.DebugFlags
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.ui.settings.state.SettingsUiEvent
import xyz.ksharma.huezoo.ui.settings.state.SettingsUiState
import xyz.ksharma.huezoo.ui.util.safeLaunch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val platformOps: PlatformOps,
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
            SettingsUiEvent.ResetAllTapped -> _uiState.update { it.copy(showResetConfirm = true) }
            SettingsUiEvent.ResetAllDismissed -> _uiState.update { it.copy(showResetConfirm = false) }
            SettingsUiEvent.ResetAllConfirmed -> resetAll()
            is SettingsUiEvent.NameInputChanged -> _uiState.update { it.copy(nameInput = event.value) }
            SettingsUiEvent.SaveNameTapped -> saveName()
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

    private fun resetAll() {
        safeLaunch {
            settingsRepository.resetAll()
            load()
            _uiState.update { it.copy(showResetConfirm = false) }
        }
    }
}
