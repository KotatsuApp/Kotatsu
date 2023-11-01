package org.koitharu.kotatsu.core.util

import androidx.collection.ArrayMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

@Deprecated("", replaceWith = ReplaceWith("CompositeMutex2"))
class CompositeMutex<T : Any> : Set<T> {

	private val state = ArrayMap<T, MutableStateFlow<Boolean>>()
	private val mutex = Mutex()

	override val size: Int
		get() = state.size

	override fun contains(element: T): Boolean {
		return state.containsKey(element)
	}

	override fun containsAll(elements: Collection<T>): Boolean {
		return elements.all { x -> state.containsKey(x) }
	}

	override fun isEmpty(): Boolean {
		return state.isEmpty()
	}

	override fun iterator(): Iterator<T> {
		return state.keys.iterator()
	}

	suspend fun lock(element: T) {
		while (coroutineContext.isActive) {
			waitForRemoval(element)
			mutex.withLock {
				if (state[element] == null) {
					state[element] = MutableStateFlow(false)
					return
				}
			}
		}
	}

	fun unlock(element: T) {
		checkNotNull(state.remove(element)) {
			"CompositeMutex is not locked for $element"
		}.value = true
	}

	private suspend fun waitForRemoval(element: T) {
		val flow = state[element] ?: return
		flow.first { it }
	}
}
