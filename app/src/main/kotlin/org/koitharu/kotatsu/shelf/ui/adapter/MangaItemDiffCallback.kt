package org.koitharu.kotatsu.shelf.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import kotlin.jvm.internal.Intrinsics

class MangaItemDiffCallback : DiffUtil.ItemCallback<ListModel>() {

	override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
		oldItem as MangaItemModel
		newItem as MangaItemModel
		return oldItem.javaClass == newItem.javaClass && oldItem.id == newItem.id
	}

	override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
		return Intrinsics.areEqual(oldItem, newItem)
	}

	override fun getChangePayload(oldItem: ListModel, newItem: ListModel): Any? {
		oldItem as MangaItemModel
		newItem as MangaItemModel
		return when {
			oldItem.progress != newItem.progress -> MangaListAdapter.PAYLOAD_PROGRESS
			oldItem.counter != newItem.counter -> Unit
			else -> super.getChangePayload(oldItem, newItem)
		}
	}
}
