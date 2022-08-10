package org.koitharu.kotatsu.utils.ext

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

fun <T> Flow<T>.onFirst(action: suspend (T) -> Unit): Flow<T> {
	var isFirstCall = true
	return onEach {
		if (isFirstCall) {
			action(it)
			isFirstCall = false
		}
	}
}

inline fun <T, R> Flow<List<T>>.mapItems(crossinline transform: (T) -> R): Flow<List<R>> {
	return map { list -> list.map(transform) }
}

fun <T> Flow<T>.throttle(timeoutMillis: (T) -> Long): Flow<T> {
	var lastEmittedAt = 0L
	return transformLatest { value ->
		val delay = timeoutMillis(value)
		val now = SystemClock.elapsedRealtime()
		if (delay > 0L) {
			if (lastEmittedAt + delay < now) {
				delay(lastEmittedAt + delay - now)
			}
		}
		emit(value)
		lastEmittedAt = now
	}
}

fun <T> StateFlow<T?>.requireValue(): T = checkNotNull(value) {
	"StateFlow value is null"
}
