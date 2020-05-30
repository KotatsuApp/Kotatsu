package org.koitharu.kotatsu.ui.common.list

import android.view.ViewGroup

class ProgressBarAdapter : BaseRecyclerAdapter<Boolean, Unit>() {

	var isVisible: Boolean
		get() = dataSet.isNotEmpty()
		set(value) {
			if (value == dataSet.isEmpty()) {
				if (value) {
					appendItem(true)
				} else {
					removeItemAt(0)
				}
			}
		}

	override fun getExtra(item: Boolean, position: Int) = Unit

	override fun onCreateViewHolder(parent: ViewGroup) = ProgressBarHolder(parent)

	override fun onGetItemId(item: Boolean) = 1L
}