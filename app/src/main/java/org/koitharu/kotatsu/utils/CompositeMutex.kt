package org.koitharu.kotatsu.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.coroutines.resume

class CompositeMutex<T : Any> : Set<T> {

	private val data = HashMap<T, MutableList<CancellableContinuation<Unit>>>()
	private val mutex = Mutex()

	override val size: Int
		get() = data.size

	override fun contains(element: T): Boolean {
		return data.containsKey(element)
	}

	override fun containsAll(elements: Collection<T>): Boolean {
		return elements.all { x -> data.containsKey(x) }
	}

	override fun isEmpty(): Boolean {
		return data.isEmpty()
	}

	override fun iterator(): Iterator<T> {
		return data.keys.iterator()
	}

	suspend fun lock(element: T) {
		waitForRemoval(element)
		mutex.withLock {
			val lastValue = data.put(element, LinkedList<CancellableContinuation<Unit>>())
			check(lastValue == null) {
				"CompositeMutex is double-locked for $element"
			}
		}
	}

	suspend fun unlock(element: T) {
		val continuations = mutex.withLock {
			checkNotNull(data.remove(element)) {
				"CompositeMutex is not locked for $element"
			}
		}
		continuations.forEach { c ->
			if (c.isActive) {
				c.resume(Unit)
			}
		}
	}

	private suspend fun waitForRemoval(element: T) {
		val list = data[element] ?: return
		suspendCancellableCoroutine<Unit> { continuation ->
			list.add(continuation)
			continuation.invokeOnCancellation {
				list.remove(continuation)
			}
		}
	}
}