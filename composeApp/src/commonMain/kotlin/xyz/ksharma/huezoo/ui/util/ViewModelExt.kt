package xyz.ksharma.huezoo.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Launches a coroutine on [viewModelScope] with a [CoroutineExceptionHandler] that logs
 * uncaught exceptions instead of crashing the app.
 *
 * Use this instead of bare [viewModelScope.launch] for any coroutine that performs I/O,
 * state mutations, or other work that may throw unexpectedly.
 *
 * @param tag  Label printed with the error (defaults to the ViewModel's class name).
 * @param block  The suspend work to run.
 */
fun ViewModel.safeLaunch(
    tag: String = this::class.simpleName ?: "ViewModel",
    block: suspend CoroutineScope.() -> Unit,
): Job = viewModelScope.launch(
    context = CoroutineExceptionHandler { _, throwable ->
        println("[ERROR] $tag: Uncaught coroutine exception — ${throwable.message}")
    },
    block = block,
)
