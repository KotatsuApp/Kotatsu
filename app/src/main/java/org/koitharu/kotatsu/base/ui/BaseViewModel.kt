package org.koitharu.kotatsu.base.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.*
import org.koitharu.kotatsu.base.ui.util.CountedBooleanLiveData
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

abstract class BaseViewModel : ViewModel() {

	val onError = SingleLiveEvent<Throwable>()
	val isLoading = CountedBooleanLiveData()

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
		isLoading.postValue(true)
		try {
			block()
		} finally {
			isLoading.postValue(false)
		}
	}

	private fun createErrorHandler() = CoroutineExceptionHandler { _, throwable ->
		throwable.printStackTraceDebug()
		if (throwable !is CancellationException) {
			onError.postCall(throwable)
		}
	}
}