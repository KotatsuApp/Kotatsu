package org.koitharu.kotatsu.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

class LiveStateFlow<T>(
	private val stateFlow: StateFlow<T>,
	private val job: Job,
) : StateFlow<T> by stateFlow, Job by job {


}