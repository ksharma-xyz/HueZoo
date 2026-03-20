package xyz.ksharma.huezoo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.ui.settings.state.SettingsUiEvent
import xyz.ksharma.huezoo.ui.settings.state.SettingsUiState

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
            SettingsUiEvent.ResetAllTapped -> _uiState.update { it.copy(showResetConfirm = true) }
            SettingsUiEvent.ResetAllDismissed -> _uiState.update { it.copy(showResetConfirm = false) }
            SettingsUiEvent.ResetAllConfirmed -> resetAll()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val isPaid = settingsRepository.isPaid()
            val gems = settingsRepository.getGems()
            _uiState.update {
                it.copy(
                    isPaid = isPaid,
                    gems = gems,
                    isDebugBuild = platformOps.isDebugBuild,
                )
            }
        }
    }

    private fun togglePaid() {
        viewModelScope.launch {
            val newPaid = !_uiState.value.isPaid
            settingsRepository.setPaid(newPaid)
            _uiState.update { it.copy(isPaid = newPaid) }
        }
    }

    private fun resetAll() {
        viewModelScope.launch {
            settingsRepository.resetAll()
            load()
            _uiState.update { it.copy(showResetConfirm = false) }
        }
    }
}

