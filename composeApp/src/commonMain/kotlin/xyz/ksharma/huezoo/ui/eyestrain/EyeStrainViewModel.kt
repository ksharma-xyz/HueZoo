package xyz.ksharma.huezoo.ui.eyestrain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.data.repository.SettingsRepository

class EyeStrainViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _navEvent = MutableSharedFlow<EyeStrainNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<EyeStrainNavEvent> = _navEvent.asSharedFlow()

    fun onGotIt() {
        viewModelScope.launch {
            settingsRepository.setSeenHealthNotice()
            _navEvent.emit(EyeStrainNavEvent.NavigateToHome)
        }
    }
}

sealed interface EyeStrainNavEvent {
    data object NavigateToHome : EyeStrainNavEvent
}
