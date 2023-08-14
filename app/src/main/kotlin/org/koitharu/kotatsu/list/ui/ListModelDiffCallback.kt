package org.koitharu.kotatsu.list.ui

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.list.ui.model.ListModel

open class ListModelDiffCallback<T : ListModel> : DiffUtil.ItemCallback<T>() {

	override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
		return oldItem.areItemsTheSame(newItem)
	}

	override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
		return oldItem == newItem
	}

	override fun getChangePayload(oldItem: T, newItem: T): Any? {
		return newItem.getChangePayload(oldItem)
	}

	companion object : ListModelDiffCallback<ListModel>() {

		val PAYLOAD_CHECKED_CHANGED = Any()
		val PAYLOAD_NESTED_LIST_CHANGED = Any()
		val PAYLOAD_PROGRESS_CHANGED = Any()
		val PAYLOAD_ANYTHING_CHANGED = Any()
	}
}
