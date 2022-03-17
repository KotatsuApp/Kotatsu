package org.koitharu.kotatsu.list.ui.adapter

import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.util.valueIterator
import org.koitharu.kotatsu.BuildConfig
import java.util.*

class AsyncViewFactory(private val parent: ViewGroup) : AsyncLayoutInflater.OnInflateFinishedListener {

	private val asyncInflater = AsyncLayoutInflater(parent.context)
	private val pool = SparseArray<LinkedList<View>>()

	override fun onInflateFinished(view: View, resid: Int, parent: ViewGroup?) {
		var list = pool.get(resid)
		if (list != null) {
			list.addLast(view)
		} else {
			list = LinkedList()
			list.add(view)
			pool.put(resid, list)
		}
	}

	fun clear() {
		if (BuildConfig.DEBUG) {
			pool.valueIterator().forEach {
				if (it.isNotEmpty()) {
					Log.w("AsyncViewFactory", "You have ${it.size} unconsumed prefetched items")
				}
			}
		}
		pool.clear()
	}

	fun prefetch(@LayoutRes resId: Int, count: Int) {
		if (count <= 0) return
		repeat(count) {
			asyncInflater.inflate(resId, parent, this)
		}
	}

	operator fun get(@LayoutRes resId: Int): View? {
		val result = pool.get(resId)?.removeFirstOrNull()
		if (BuildConfig.DEBUG && result == null) {
			Log.w("AsyncViewFactory", "Item requested but missing")
		}
		return result
	}

	fun getCount(@LayoutRes resId: Int): Int {
		return pool[resId]?.size ?: 0
	}
}