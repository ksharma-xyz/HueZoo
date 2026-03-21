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
            is SettingsUiEvent.NameInputChanged -> _uiState.update { it.copy(nameInput = event.value) }
            SettingsUiEvent.SaveNameTapped -> saveName()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val isPaid = settingsRepository.isPaid()
            val gems = settingsRepository.getGems()
            val userName = settingsRepository.getUserName()
            _uiState.update {
                it.copy(
                    isPaid = isPaid,
                    gems = gems,
                    isDebugBuild = platformOps.isDebugBuild,
                    userName = userName,
                    nameInput = userName ?: "",
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

    private fun saveName() {
        val name = _uiState.value.nameInput.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            settingsRepository.setUserName(name)
            _uiState.update { it.copy(userName = name) }
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
