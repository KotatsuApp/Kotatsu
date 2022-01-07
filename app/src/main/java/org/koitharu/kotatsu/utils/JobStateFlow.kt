package org.koitharu.kotatsu.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn

class JobStateFlow<S>(
	private val stateFlow: StateFlow<S>,
	private val job: Job,
) : StateFlow<S> by stateFlow, Job by job {

	suspend fun collectAndJoin(): Unit {
		coroutineScope {
			val collectJob = launchIn(this)
			join()
			collectJob.cancelAndJoin()
		}
	}
}