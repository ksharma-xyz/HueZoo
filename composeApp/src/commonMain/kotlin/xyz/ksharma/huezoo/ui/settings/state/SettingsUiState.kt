package xyz.ksharma.huezoo.ui.settings.state

data class SettingsUiState(
    val isPaid: Boolean = false,
    val gems: Int = 0,
    val isDebugBuild: Boolean = false,
    /** Controls the "RESET ALL — ARE YOU SURE?" confirmation card. */
    val showResetConfirm: Boolean = false,
)

