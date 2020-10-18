package org.koitharu.kotatsu.ui.base

import kotlinx.coroutines.*
import moxy.MvpPresenter
import moxy.presenterScope
import org.koin.core.component.KoinComponent
import org.koitharu.kotatsu.BuildConfig
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BasePresenter<V : BaseMvpView> : MvpPresenter<V>(), KoinComponent {

	protected fun launchJob(
		context: CoroutineContext = EmptyCoroutineContext,
		start: CoroutineStart = CoroutineStart.DEFAULT,
		block: suspend CoroutineScope.() -> Unit
	) {
		presenterScope.launch(context + createErrorHandler(), start, block)
	}

	protected fun launchLoadingJob(
		context: CoroutineContext = EmptyCoroutineContext,
		start: CoroutineStart = CoroutineStart.DEFAULT,
		block: suspend CoroutineScope.() -> Unit
	) {
		presenterScope.launch(context + createErrorHandler(), start) {
			viewState.onLoadingStateChanged(isLoading = true)
			try {
				block()
			} finally {
				viewState.onLoadingStateChanged(isLoading = false)
			}
		}
	}

	private fun createErrorHandler() = CoroutineExceptionHandler { _, throwable ->
		if (BuildConfig.DEBUG) {
			throwable.printStackTrace()
		}
		if (throwable !is CancellationException) {
			viewState.onError(throwable)
		}
	}
}