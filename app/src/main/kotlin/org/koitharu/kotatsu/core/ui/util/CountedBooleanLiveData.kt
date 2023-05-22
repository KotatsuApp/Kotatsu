package org.koitharu.kotatsu.core.ui.util

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import java.util.concurrent.atomic.AtomicInteger

class CountedBooleanLiveData : LiveData<Boolean>(false) {

	private val counter = AtomicInteger(0)

	@AnyThread
	fun increment() {
		if (counter.getAndIncrement() == 0) {
			postValue(true)
		}
	}

	@AnyThread
	fun decrement() {
		if (counter.decrementAndGet() == 0) {
			postValue(false)
		}
	}

	@AnyThread
	fun reset() {
		if (counter.getAndSet(0) != 0) {
			postValue(false)
		}
	}
}
