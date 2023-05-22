package org.koitharu.kotatsu.core.util.progress

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ProgressDeferred<T, P>(
	private val deferred: Deferred<T>,
	private val progress: StateFlow<P>,
) : Deferred<T> by deferred {

	val progressValue: P
		get() = progress.value

	fun progressAsFlow(): Flow<P> = progress
}
