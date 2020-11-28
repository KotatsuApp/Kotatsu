package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T?>.observeNotNull(owner: LifecycleOwner, observer: Observer<T>) {
	this.observe(owner) {
		if (it != null) {
			observer.onChanged(it)
		}
	}
}