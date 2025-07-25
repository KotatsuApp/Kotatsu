package org.koitharu.kotatsu.core.util.ext

import android.os.SystemClock
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.suspendlazy.SuspendLazy
import java.util.concurrent.TimeUnit
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

fun <T> Flow<T>.throttle(timeoutMillis: Long): Flow<T> = throttle { timeoutMillis }

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

fun tickerFlow(interval: Long, timeUnit: TimeUnit): Flow<Long> = flow {
	while (true) {
		emit(SystemClock.elapsedRealtime())
		delay(timeUnit.toMillis(interval))
	}
}

fun <T> Flow<T>.withTicker(interval: Long, timeUnit: TimeUnit) = channelFlow<T> {
	onCompletion { cause ->
		close(cause)
	}.combine(tickerFlow(interval, timeUnit)) { x, _ -> x }
		.transformWhile<T, Unit> { trySend(it).isSuccess }
		.collect()
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

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
	flow: Flow<T1>,
	flow2: Flow<T2>,
	flow3: Flow<T3>,
	flow4: Flow<T4>,
	flow5: Flow<T5>,
	flow6: Flow<T6>,
	flow7: Flow<T7>,
	transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
	transform(
		args[0] as T1,
		args[1] as T2,
		args[2] as T3,
		args[3] as T4,
		args[4] as T5,
		args[5] as T6,
		args[6] as T7,
	)
}

suspend fun <T : Any> Flow<T?>.firstNotNull(): T = checkNotNull(first { x -> x != null })

suspend fun <T : Any> Flow<T?>.firstNotNullOrNull(): T? = firstOrNull { x -> x != null }

fun <T> Flow<Flow<T>>.flattenLatest() = flatMapLatest { it }

fun <T> SuspendLazy<T>.asFlow() = flow { emit(runCatchingCancellable { get() }) }

suspend fun <T> SendChannel<T>.sendNotNull(item: T?) {
	if (item != null) {
		send(item)
	}
}

fun <T> MutableStateFlow<List<T>>.append(item: T) {
	update { list -> list + item }
}

fun <T> Flow<T>.concat(other: Flow<T>) = flow {
	emitAll(this@concat)
	emitAll(other)
}
