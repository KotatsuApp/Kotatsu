package org.koitharu.kotatsu.core.util.progress

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

open class ProgressJob<P>(
	private val job: Job,
	private val progress: StateFlow<P>,
) : Job by job {

	val progressValue: P
		get() = progress.value

	fun progressAsFlow(): Flow<P> = progress
}
