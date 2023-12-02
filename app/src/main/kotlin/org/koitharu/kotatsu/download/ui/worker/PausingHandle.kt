package org.koitharu.kotatsu.download.ui.worker

import androidx.annotation.AnyThread
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class PausingHandle : AbstractCoroutineContextElement(PausingHandle) {

	private val paused = MutableStateFlow(false)
	private val isSkipError = MutableStateFlow(false)

	@get:AnyThread
	val isPaused: Boolean
		get() = paused.value

	@AnyThread
	suspend fun awaitResumed() {
		paused.first { !it }
	}

	@AnyThread
	fun pause() {
		paused.value = true
	}

	@AnyThread
	fun resume(skipError: Boolean) {
		isSkipError.value = skipError
		paused.value = false
	}

	suspend fun yield() {
		if (paused.value) {
			paused.first { !it }
		}
	}

	fun skipCurrentError(): Boolean = isSkipError.compareAndSet(expect = true, update = false)

	companion object : CoroutineContext.Key<PausingHandle> {

		suspend fun current() = checkNotNull(currentCoroutineContext()[this]) {
			"PausingHandle not found in current context"
		}
	}
}
