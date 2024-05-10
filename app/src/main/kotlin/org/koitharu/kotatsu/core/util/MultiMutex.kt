package org.koitharu.kotatsu.core.util

import androidx.collection.ArrayMap
import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MultiMutex<T : Any> : Set<T> {

	private val delegates = ArrayMap<T, Mutex>()

	override val size: Int
		get() = delegates.size

	override fun contains(element: T): Boolean = synchronized(delegates) {
		delegates.containsKey(element)
	}

	override fun containsAll(elements: Collection<T>): Boolean = synchronized(delegates) {
		elements.all { x -> delegates.containsKey(x) }
	}

	override fun isEmpty(): Boolean {
		return delegates.isEmpty()
	}

	override fun iterator(): Iterator<T> {
		return delegates.keys.iterator()
	}

	suspend fun lock(element: T) {
		val mutex = synchronized(delegates) {
			delegates.getOrPut(element) {
				Mutex()
			}
		}
		mutex.lock()
	}

	fun unlock(element: T) {
		synchronized(delegates) {
			delegates.remove(element)?.unlock()
		}
	}

	suspend inline fun <R> withLock(element: T, block: () -> R): R {
		contract {
			callsInPlace(block, InvocationKind.EXACTLY_ONCE)
		}
		return try {
			lock(element)
			block()
		} finally {
			unlock(element)
		}
	}
}
