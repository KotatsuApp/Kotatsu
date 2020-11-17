package org.koitharu.kotatsu.base.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.utils.SingleLiveEvent

abstract class BaseViewModel : ViewModel() {

	val onError = SingleLiveEvent<Throwable>()
	val isLoading = MutableLiveData(false)

	protected fun launchJob(
		start: CoroutineStart = CoroutineStart.DEFAULT,
		block: suspend CoroutineScope.() -> Unit
	): Job = viewModelScope.launch(createErrorHandler(), start, block)

	protected fun launchLoadingJob(
		start: CoroutineStart = CoroutineStart.DEFAULT,
		block: suspend CoroutineScope.() -> Unit
	): Job = viewModelScope.launch(createErrorHandler(), start) {
		isLoading.value = true
		try {
			block()
		} finally {
			isLoading.value = false
		}
	}

	private fun createErrorHandler() = CoroutineExceptionHandler { _, throwable ->
		if (BuildConfig.DEBUG) {
			throwable.printStackTrace()
		}
		if (throwable !is CancellationException) {
			onError.call(throwable)
		}
	}
}