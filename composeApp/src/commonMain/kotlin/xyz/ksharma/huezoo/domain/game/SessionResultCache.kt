package xyz.ksharma.huezoo.domain.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.huezoo.navigation.SessionResult

/**
 * Singleton in-memory cache that ferries the latest game session result from a game ViewModel
 * to [ResultViewModel], bypassing nav args entirely.
 *
 * Game ViewModels write here just before emitting their NavigateToResult nav event.
 * ResultViewModel collects [result] reactively — any new non-null value triggers a reload,
 * so ViewModel reuse across "play again" sessions is handled automatically.
 */
class SessionResultCache {

    private val _result = MutableStateFlow<SessionResult?>(null)
    val result: StateFlow<SessionResult?> = _result.asStateFlow()

    fun set(sessionResult: SessionResult) {
        _result.value = sessionResult
    }
}
