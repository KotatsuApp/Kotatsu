package org.koitharu.kotatsu.core.util

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class MultiMutex<T : Any> {

	private val delegates = ConcurrentHashMap<T, Mutex>()

	@VisibleForTesting
	val size: Int
		get() = delegates.count { it.value.isLocked }

	fun isNotEmpty() = delegates.any { it.value.isLocked }

	fun isEmpty() = delegates.none { it.value.isLocked }

	suspend fun lock(element: T) {
		val mutex = delegates.computeIfAbsent(element) { Mutex() }
		mutex.lock()
	}

	fun unlock(element: T) {
		delegates[element]?.unlock()
	}

	suspend inline fun <R> withLock(element: T, block: () -> R): R {
		contract {
			callsInPlace(block, InvocationKind.EXACTLY_ONCE)
		}
		lock(element)
		return try {
			block()
		} finally {
			unlock(element)
		}
	}
}
