package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
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
