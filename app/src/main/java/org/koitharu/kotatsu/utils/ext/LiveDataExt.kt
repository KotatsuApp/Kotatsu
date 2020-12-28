package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.koitharu.kotatsu.utils.BufferedObserver
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> LiveData<T?>.observeNotNull(owner: LifecycleOwner, observer: Observer<T>) {
	this.observe(owner) {
		if (it != null) {
			observer.onChanged(it)
		}
	}
}

fun <T> LiveData<T>.observeWithPrevious(owner: LifecycleOwner, observer: BufferedObserver<T>) {
	var previous: T? = null
	this.observe(owner) {
		observer.onChanged(it, previous)
		previous = it
	}
}

fun <T> Flow<T>.asLiveDataDistinct(
	context: CoroutineContext = EmptyCoroutineContext
): LiveData<T> = liveData(context) {
	collect {
		if (it != latestValue) {
			emit(it)
		}
	}
}

fun <T> Flow<T>.asLiveDataDistinct(
	context: CoroutineContext = EmptyCoroutineContext,
	defaultValue: T
): LiveData<T> = liveData(context, 0L) {
	if (latestValue == null) {
		emit(defaultValue)
	}
	collect {
		if (it != latestValue) {
			emit(it)
		}
	}
}

fun <T> Flow<T>.asLiveDataDistinct(
	context: CoroutineContext = EmptyCoroutineContext,
	defaultValue: suspend () -> T
): LiveData<T> = liveData(context) {
	if (latestValue == null) {
		emit(defaultValue())
	}
	collect {
		if (it != latestValue) {
			emit(it)
		}
	}
}