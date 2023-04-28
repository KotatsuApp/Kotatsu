package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.utils.BufferedObserver
import kotlin.coroutines.EmptyCoroutineContext

fun <T> LiveData<T>.requireValue(): T = checkNotNull(value) {
	"LiveData value is null"
}

fun <T> LiveData<T>.observeWithPrevious(owner: LifecycleOwner, observer: BufferedObserver<T>) {
	var previous: T? = null
	this.observe(owner) {
		observer.onChanged(it, previous)
		previous = it
	}
}

suspend fun <T> MutableLiveData<T>.emitValue(newValue: T) {
	val dispatcher = Dispatchers.Main.immediate
	if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
		withContext(dispatcher) {
			value = newValue
		}
	} else {
		value = newValue
	}
}
