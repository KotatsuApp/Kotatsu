package org.koitharu.kotatsu.core.util

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val DEFAULT_TIMEOUT = 5_000L

/**
 * Similar to a CoroutineLiveData but optimized for using within infinite flows
 */
class FlowLiveData<T>(
	private val flow: Flow<T>,
	defaultValue: T,
	context: CoroutineContext = EmptyCoroutineContext,
	private val timeoutInMs: Long = DEFAULT_TIMEOUT,
) : LiveData<T>(defaultValue) {

	private val scope = CoroutineScope(Dispatchers.Main.immediate + context + SupervisorJob(context[Job]))
	private var job: Job? = null
	private var cancellationJob: Job? = null

	override fun onActive() {
		super.onActive()
		cancellationJob?.cancel()
		cancellationJob = null
		if (job?.isActive == true) {
			return
		}
		job = scope.launch {
			flow.collect(Collector())
		}
	}

	override fun onInactive() {
		super.onInactive()
		cancellationJob?.cancel()
		cancellationJob = scope.launch(Dispatchers.Main.immediate) {
			delay(timeoutInMs)
			if (!hasActiveObservers()) {
				job?.cancel()
				job = null
			}
		}
	}

	private inner class Collector : FlowCollector<T> {

		private var previousValue: Any? = value
		private val dispatcher = Dispatchers.Main.immediate

		override suspend fun emit(value: T) {
			if (previousValue != value) {
				previousValue = value
				if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
					withContext(dispatcher) {
						setValue(value)
					}
				} else {
					setValue(value)
				}
			}
		}
	}
}

fun <T> Flow<T>.asFlowLiveData(
	context: CoroutineContext = EmptyCoroutineContext,
	defaultValue: T,
	timeoutInMs: Long = DEFAULT_TIMEOUT,
): LiveData<T> = FlowLiveData(this, defaultValue, context, timeoutInMs)

fun <T> StateFlow<T>.asFlowLiveData(
	context: CoroutineContext = EmptyCoroutineContext,
	timeoutInMs: Long = DEFAULT_TIMEOUT,
): LiveData<T> = FlowLiveData(this, value, context, timeoutInMs)
