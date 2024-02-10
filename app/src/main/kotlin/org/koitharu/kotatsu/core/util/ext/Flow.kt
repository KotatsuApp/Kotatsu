package org.koitharu.kotatsu.core.util.ext

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import org.koitharu.kotatsu.R
import java.util.concurrent.atomic.AtomicInteger

fun <T> Flow<T>.onFirst(action: suspend (T) -> Unit): Flow<T> {
	var isFirstCall = true
	return onEach {
		if (isFirstCall) {
			action(it)
			isFirstCall = false
		}
	}.onCompletion {
		isFirstCall = true
	}
}

fun <T> Flow<T>.onEachWhile(action: suspend (T) -> Boolean): Flow<T> {
	var isCalled = false
	return onEach {
		if (!isCalled) {
			isCalled = action(it)
		}
	}.onCompletion {
		isCalled = false
	}
}

fun <T> Flow<T>.onEachIndexed(action: suspend (index: Int, T) -> Unit): Flow<T> {
	val counter = AtomicInteger(0)
	return transform { value ->
		action(counter.getAndIncrement(), value)
		return@transform emit(value)
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

fun <T> Flow<Collection<T>>.flatten(): Flow<T> = flow {
	collect { value ->
		for (item in value) {
			emit(item)
		}
	}
}

fun <T> Flow<T>.zipWithPrevious(): Flow<Pair<T?, T>> = flow {
	var previous: T? = null
	collect { value ->
		val result = previous to value
		previous = value
		emit(result)
	}
}

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, R> combine(
	flow: Flow<T1>,
	flow2: Flow<T2>,
	flow3: Flow<T3>,
	flow4: Flow<T4>,
	flow5: Flow<T5>,
	flow6: Flow<T6>,
	transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
	transform(
		args[0] as T1,
		args[1] as T2,
		args[2] as T3,
		args[3] as T4,
		args[4] as T5,
		args[5] as T6,
	)
}

suspend fun <T : Any> Flow<T?>.firstNotNull(): T = checkNotNull(first { x -> x != null })

suspend fun <T : Any> Flow<T?>.firstNotNullOrNull(): T? = firstOrNull { x -> x != null }
