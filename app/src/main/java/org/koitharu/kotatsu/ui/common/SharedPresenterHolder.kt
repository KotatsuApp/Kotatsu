package org.koitharu.kotatsu.ui.common

import android.util.ArrayMap
import moxy.MvpPresenter
import java.lang.ref.WeakReference

abstract class SharedPresenterHolder<T : MvpPresenter<*>> {

	private val cache = ArrayMap<Int, WeakReference<T>>(3)

	fun getInstance(key: Int): T {
		var instance = cache[key]?.get()
		if (instance == null) {
			instance = onCreatePresenter(key)
			cache[key] = WeakReference(instance)
		}
		return instance
	}

	fun clear(key: Int) {
		cache.remove(key)
	}

	protected abstract fun onCreatePresenter(key: Int): T
}