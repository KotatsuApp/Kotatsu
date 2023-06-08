package org.koitharu.kotatsu.core.util

import android.util.Log
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver

class LoggingAdapterDataObserver(
	private val tag: String,
) : AdapterDataObserver() {

	override fun onChanged() {
		Log.d(tag, "onChanged()")
	}

	override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
		Log.d(tag, "onItemRangeChanged(positionStart=$positionStart, itemCount=$itemCount)")
	}

	override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
		Log.d(tag, "onItemRangeChanged(positionStart=$positionStart, itemCount=$itemCount, payload=$payload)")
	}

	override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
		Log.d(tag, "onItemRangeInserted(positionStart=$positionStart, itemCount=$itemCount)")
	}

	override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
		Log.d(tag, "onItemRangeRemoved(positionStart=$positionStart, itemCount=$itemCount)")
	}

	override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
		Log.d(tag, "onItemRangeMoved(fromPosition=$fromPosition, toPosition=$toPosition, itemCount=$itemCount)")
	}

	override fun onStateRestorationPolicyChanged() {
		Log.d(tag, "onStateRestorationPolicyChanged()")
	}
}
