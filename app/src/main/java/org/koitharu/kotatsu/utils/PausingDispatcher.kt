package org.koitharu.kotatsu.utils

import androidx.annotation.MainThread
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable

class PausingDispatcher(
	private val dispatcher: CoroutineDispatcher,
) : CoroutineDispatcher() {

	@Volatile
	private var isPaused = false
	private val queue = ConcurrentLinkedQueue<Task>()

	override fun isDispatchNeeded(context: CoroutineContext): Boolean {
		return isPaused || super.isDispatchNeeded(context)
	}

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		if (isPaused) {
			queue.add(Task(context, block))
		} else {
			dispatcher.dispatch(context, block)
		}
	}

	@MainThread
	fun pause() {
		isPaused = true
	}

	@MainThread
	fun resume() {
		if (!isPaused) {
			return
		}
		isPaused = false
		while (true) {
			val task = queue.poll() ?: break
			dispatcher.dispatch(task.context, task.block)
		}
	}

	private class Task(
		val context: CoroutineContext,
		val block: Runnable,
	)
}