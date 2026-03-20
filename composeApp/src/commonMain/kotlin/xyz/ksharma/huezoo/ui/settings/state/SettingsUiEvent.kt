package xyz.ksharma.huezoo.ui.settings.state

sealed interface SettingsUiEvent {
    data object TogglePaid : SettingsUiEvent
    data object ResetAllTapped : SettingsUiEvent
    data object ResetAllConfirmed : SettingsUiEvent
    data object ResetAllDismissed : SettingsUiEvent
}

