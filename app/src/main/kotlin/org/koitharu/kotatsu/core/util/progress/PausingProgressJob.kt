package org.koitharu.kotatsu.core.util.progress

import androidx.annotation.AnyThread
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.koitharu.kotatsu.download.ui.worker.PausingHandle

class PausingProgressJob<P>(
	job: Job,
	progress: StateFlow<P>,
	private val pausingHandle: PausingHandle,
) : ProgressJob<P>(job, progress) {

	@get:AnyThread
	val isPaused: Boolean
		get() = pausingHandle.isPaused

	@AnyThread
	suspend fun awaitResumed() = pausingHandle.awaitResumed()

	@AnyThread
	fun pause() = pausingHandle.pause()

	@AnyThread
	fun resume() = pausingHandle.resume()
}
