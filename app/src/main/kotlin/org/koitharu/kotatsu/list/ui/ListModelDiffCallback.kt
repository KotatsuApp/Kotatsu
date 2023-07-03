package org.koitharu.kotatsu.list.ui

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.list.ui.model.ListModel

object ListModelDiffCallback : DiffUtil.ItemCallback<ListModel>() {

	val PAYLOAD_CHECKED_CHANGED = Any()
	val PAYLOAD_NESTED_LIST_CHANGED = Any()

	override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
		return oldItem.areItemsTheSame(newItem)
	}

	override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
		return oldItem == newItem
	}

	override fun getChangePayload(oldItem: ListModel, newItem: ListModel): Any? {
		return newItem.getChangePayload(oldItem)
	}
}
