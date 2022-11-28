package org.koitharu.kotatsu.utils

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

abstract class MediatorStateFlow<T> : StateFlow<T> {

	@Suppress("LeakingThis")
	private val delegate = MutableStateFlow(initialValue)
	private val collectors = AtomicInteger(0)

	protected abstract val initialValue: T

	final override val replayCache: List<T>
		get() = delegate.replayCache

	final override val value: T
		get() = delegate.value

	final override suspend fun collect(collector: FlowCollector<T>): Nothing {
		try {
			if (collectors.getAndIncrement() == 0) {
				onActive()
			}
			delegate.collect(collector)
		} finally {
			if (collectors.decrementAndGet() == 0) {
				onInactive()
			}
		}
	}

	protected fun publishValue(v: T) {
		delegate.value = v
	}

	abstract fun onActive()

	abstract fun onInactive()
}
