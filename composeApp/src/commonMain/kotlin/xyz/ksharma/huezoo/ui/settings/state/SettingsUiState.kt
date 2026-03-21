package xyz.ksharma.huezoo.ui.settings.state

data class SettingsUiState(
    val isPaid: Boolean = false,
    val gems: Int = 0,
    val isDebugBuild: Boolean = false,
    /** Controls the "RESET ALL — ARE YOU SURE?" confirmation card. */
    val showResetConfirm: Boolean = false,
    /** Current saved display name, null if not yet set. */
    val userName: String? = null,
    /** Draft value in the name text field while editing. */
    val nameInput: String = "",
)
