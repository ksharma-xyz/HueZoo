package xyz.ksharma.huezoo.ui.home.state

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Ready(
        val threshold: ThresholdCardData,
        val daily: DailyCardData,
        val isPaid: Boolean,
    ) : HomeUiState
}

data class ThresholdCardData(
    val personalBestDeltaE: Float?,
    val attemptsRemaining: Int,
    val maxAttempts: Int,
    val isBlocked: Boolean,
)

data class DailyCardData(
    val isCompletedToday: Boolean,
    val todayScore: Float?,
)
