package org.koitharu.kotatsu.core.util

import androidx.collection.ArrayMap
import kotlinx.coroutines.sync.Mutex

class CompositeMutex2<T : Any> : Set<T> {

	private val delegates = ArrayMap<T, Mutex>()

	override val size: Int
		get() = delegates.size

	override fun contains(element: T): Boolean {
		return delegates.containsKey(element)
	}

	override fun containsAll(elements: Collection<T>): Boolean {
		return elements.all { x -> delegates.containsKey(x) }
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
}
