package xyz.ksharma.huezoo.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Launches a coroutine on [viewModelScope] with a [CoroutineExceptionHandler] that records
 * uncaught exceptions to Firebase Crashlytics instead of crashing the app.
 *
 * Use this instead of bare [viewModelScope.launch] for any coroutine that performs I/O,
 * state mutations, or other work that may throw unexpectedly.
 *
 * @param block  The suspend work to run.
 */
fun ViewModel.safeLaunch(
    block: suspend CoroutineScope.() -> Unit,
): Job = viewModelScope.launch(
    context = CoroutineExceptionHandler { _, throwable ->
        Firebase.crashlytics.recordException(throwable)
    },
    block = block,
)
