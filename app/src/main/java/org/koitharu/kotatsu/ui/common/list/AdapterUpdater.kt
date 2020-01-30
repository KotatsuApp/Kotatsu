package org.koitharu.kotatsu.ui.common.list

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class AdapterUpdater<T>(oldList: List<T>, newList: List<T>, getId: (T) -> Long) {

	private val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

		override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
			getId(oldList[oldItemPosition]) == getId(newList[newItemPosition])

		override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
			Objects.equals(oldList[oldItemPosition], newList[newItemPosition])

		override fun getOldListSize() = oldList.size

		override fun getNewListSize() = newList.size
	})

	operator fun invoke(adapter: RecyclerView.Adapter<*>) {
		diff.dispatchUpdatesTo(adapter)
	}
}