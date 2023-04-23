package org.koitharu.kotatsu.base.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.base.ui.util.CountedBooleanLiveData
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel : ViewModel() {

	@JvmField
	protected val loadingCounter = CountedBooleanLiveData()

	@JvmField
	protected val errorEvent = SingleLiveEvent<Throwable>()

	val onError: LiveData<Throwable>
		get() = errorEvent

	val isLoading: LiveData<Boolean>
		get() = loadingCounter

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
			errorEvent.postCall(throwable)
		}
	}
}
