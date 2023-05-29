package org.koitharu.kotatsu.download.ui.worker

import androidx.annotation.AnyThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class PausingHandle {

	private val paused = MutableStateFlow(false)

	@get:AnyThread
	val isPaused: Boolean
		get() = paused.value

	@AnyThread
	suspend fun awaitResumed() {
		paused.filter { !it }.first()
	}

	@AnyThread
	fun pause() {
		paused.value = true
	}

	@AnyThread
	fun resume() {
		paused.value = false
	}
}
