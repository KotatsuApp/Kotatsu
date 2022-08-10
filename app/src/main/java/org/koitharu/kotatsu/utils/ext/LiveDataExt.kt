package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.koitharu.kotatsu.utils.BufferedObserver

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

fun <T> StateFlow<T>.asLiveDataDistinct(
	context: CoroutineContext = EmptyCoroutineContext,
): LiveData<T> = asLiveDataDistinct(context, value)

fun <T> Flow<T>.asLiveDataDistinct(
	context: CoroutineContext = EmptyCoroutineContext,
	defaultValue: T,
): LiveData<T> = liveData(context) {
	if (latestValue == null) {
		emit(defaultValue)
	}
	collect {
		if (it != latestValue) {
			emit(it)
		}
	}
}
