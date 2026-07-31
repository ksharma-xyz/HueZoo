package xyz.ksharma.huezoo.ui.settings.state

data class SettingsUiState(
    val isPaid: Boolean = false,
    val gems: Int = 0,
    val isDebugBuild: Boolean = false,
    /** Controls the "RESET ALL — ARE YOU SURE?" confirmation card. */
    val showResetConfirm: Boolean = false,
    /** Debug only — when true, HomeScreen shows the streak celebration animation unconditionally. */
    val forceStreakCelebration: Boolean = false,
    /** Debug only — when true, all ad composables are hidden (for screenshots). */
    val hideAds: Boolean = false,
    /** Current saved display name, null if not yet set. */
    val userName: String? = null,
    /** Draft value in the name text field while editing. */
    val nameInput: String = "",
    /** Version string shown at the bottom of the screen ("0.1.0" release, "0.1.0 dev" debug). */
    val appVersion: String = "",
    /** True while a restore-purchases check is in flight. */
    val isRestoring: Boolean = false,
    /** Shown below the Restore button after a restore attempt — success or no-purchase message. */
    val restoreMessage: String? = null,
)
