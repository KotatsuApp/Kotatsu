package org.koitharu.kotatsu.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn

class DeferredStateFlow<R, S>(
	private val stateFlow: StateFlow<S>,
	private val deferred: Deferred<R>,
) : StateFlow<S> by stateFlow, Deferred<R> by deferred {

	suspend fun collectAndAwait(): R {
		return coroutineScope {
			val collectJob = launchIn(this)
			val result = await()
			collectJob.cancelAndJoin()
			result
		}
	}
}