package org.koitharu.kotatsu.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.util.ext.EventFlow
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel : ViewModel() {

	@JvmField
	protected val loadingCounter = MutableStateFlow(0)

	@JvmField
	protected val errorEvent = MutableEventFlow<Throwable>()

	val onError: EventFlow<Throwable>
		get() = errorEvent

	val isLoading: StateFlow<Boolean>
		get() = loadingCounter.map { it > 0 }
			.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

	protected fun launchJob(
		context: CoroutineContext = EmptyCoroutineContext,
		start: CoroutineStart = CoroutineStart.DEFAULT,
		block: suspend CoroutineScope.() -> Unit
	): Job = viewModelScope.launch(context + createErrorHandler(), start, block)

	protected fun launchLoadingJob(
		context: CoroutineContext = EmptyCoroutineContext,
		start: CoroutineStart = CoroutineStart.DEFAULT,
		block: suspend CoroutineScope.() -> Unit
	): Job = viewModelScope.launch(context + createErrorHandler(), start) {
		loadingCounter.increment()
		try {
			block()
		} finally {
			loadingCounter.decrement()
		}
	}

	private fun createErrorHandler() = CoroutineExceptionHandler { _, throwable ->
		throwable.printStackTraceDebug()
		if (throwable !is CancellationException) {
			errorEvent.call(throwable)
		}
	}

	protected fun MutableStateFlow<Int>.increment() = update { it + 1 }

	protected fun MutableStateFlow<Int>.decrement() = update { it - 1 }
}
