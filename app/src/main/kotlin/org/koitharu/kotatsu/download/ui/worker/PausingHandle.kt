package org.koitharu.kotatsu.download.ui.worker

import androidx.annotation.AnyThread
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class PausingHandle : AbstractCoroutineContextElement(PausingHandle) {

	private val paused = MutableStateFlow(false)
	private val skipError = MutableStateFlow(false)

	@Volatile
	private var skipAllErrors = false

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
	fun resume() {
		skipError.value = false
		paused.value = false
	}

	@AnyThread
	fun skip() {
		skipError.value = true
		paused.value = false
	}

	@AnyThread
	fun skipAll() {
		skipAllErrors = true
		skip()
	}

	suspend fun yield() {
		if (paused.value) {
			paused.first { !it }
		}
	}

	fun skipAllErrors(): Boolean = skipAllErrors

	fun skipCurrentError(): Boolean = skipError.compareAndSet(expect = true, update = false)

	companion object : CoroutineContext.Key<PausingHandle> {

		suspend fun current() = checkNotNull(currentCoroutineContext()[this]) {
			"PausingHandle not found in current context"
		}
	}
}
