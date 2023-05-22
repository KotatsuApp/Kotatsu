package org.koitharu.kotatsu.core.ui.list

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import androidx.core.os.BundleCompat
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections
import java.util.WeakHashMap

class NestedScrollStateHandle(
	savedInstanceState: Bundle?,
	private val key: String,
) {

	private val storage: SparseArray<Parcelable?> = savedInstanceState?.let {
		BundleCompat.getSparseParcelableArray(it, key, Parcelable::class.java)
	} ?: SparseArray<Parcelable?>()
	private val controllers = Collections.newSetFromMap<Controller>(WeakHashMap())

	fun attach(recycler: RecyclerView) = Controller(recycler).also(controllers::add)

	fun onSaveInstanceState(outState: Bundle) {
		controllers.forEach {
			it.saveState()
		}
		outState.putSparseParcelableArray(key, storage)
	}

	inner class Controller(
		private val recycler: RecyclerView
	) {

		private var lastPosition: Int = -1

		fun onBind(position: Int) {
			if (position != lastPosition) {
				saveState()
				lastPosition = position
				storage[position]?.let {
					restoreState(it)
				}
			}
		}

		fun onRecycled() {
			saveState()
			lastPosition = -1
		}

		fun saveState() {
			if (lastPosition != -1) {
				storage[lastPosition] = recycler.layoutManager?.onSaveInstanceState()
			}
		}

		private fun restoreState(state: Parcelable) {
			recycler.doOnNextLayout {
				recycler.layoutManager?.onRestoreInstanceState(state)
			}
		}
	}
}
