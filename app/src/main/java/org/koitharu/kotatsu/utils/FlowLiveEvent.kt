package org.koitharu.kotatsu.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class FlowLiveEvent<T>(
	private val source: Flow<T>,
	private val context: CoroutineContext
) : LiveData<T>() {

	private val scope = CoroutineScope(
		Dispatchers.Main.immediate + context + SupervisorJob(context[Job])
	)
	private val pending = AtomicBoolean(false)
	private var collectJob: Job? = null

	override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
		super.observe(owner) {
			if (pending.compareAndSet(true, false)) {
				observer.onChanged(it)
			}
		}
	}

	override fun onActive() {
		super.onActive()
		if (collectJob == null) {
			collectJob = source.onEach {
				setValue(it)
			}.launchIn(scope)
		}
	}

	override fun onInactive() {
		collectJob?.cancel()
		collectJob = null
		super.onInactive()
	}

	override fun setValue(value: T) {
		pending.set(true)
		super.setValue(value)
	}
}