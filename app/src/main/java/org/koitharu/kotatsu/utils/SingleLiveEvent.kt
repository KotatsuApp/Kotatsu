package org.koitharu.kotatsu.utils

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

class SingleLiveEvent<T> : LiveData<T>() {

	private val pending = AtomicBoolean(false)

	override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
		super.observe(owner) {
			if (pending.compareAndSet(true, false)) {
				observer.onChanged(it)
			}
		}
	}

	override fun setValue(value: T) {
		pending.set(true)
		super.setValue(value)
	}

	@MainThread
	fun call(newValue: T) {
		setValue(newValue)
	}

	@AnyThread
	fun postCall(newValue: T) {
		postValue(newValue)
	}
}