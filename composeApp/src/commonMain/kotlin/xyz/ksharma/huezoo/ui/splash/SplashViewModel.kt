package xyz.ksharma.huezoo.ui.splash

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xyz.ksharma.huezoo.ui.util.safeLaunch
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.ui.model.PlayerState

class SplashViewModel(
    private val settingsRepository: SettingsRepository,
    private val playerState: PlayerState,
) : ViewModel() {

    private val _navEvent = MutableSharedFlow<SplashNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<SplashNavEvent> = _navEvent.asSharedFlow()

    fun onSplashFinished() {
        safeLaunch {
            // Seed reactive gem state before any screen reads LocalPlayerAccentColor
            playerState.updateGems(settingsRepository.getGems())
            val next = if (settingsRepository.hasSeenHealthNotice()) {
                SplashNavEvent.NavigateToHome
            } else {
                SplashNavEvent.NavigateToEyeStrain
            }
            _navEvent.emit(next)
        }
    }
}

sealed interface SplashNavEvent {
    data object NavigateToHome : SplashNavEvent
    data object NavigateToEyeStrain : SplashNavEvent
}
