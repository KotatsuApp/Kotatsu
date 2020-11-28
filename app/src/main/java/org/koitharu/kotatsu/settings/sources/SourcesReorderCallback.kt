package org.koitharu.kotatsu.settings.sources

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SourcesReorderCallback :
	ItemTouchHelper.SimpleCallback(ItemTouchHelper.DOWN or ItemTouchHelper.UP, 0) {

	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder
	): Boolean {
		val adapter = recyclerView.adapter as? SourcesAdapter ?: return false
		val oldPos = viewHolder.bindingAdapterPosition
		val newPos = target.bindingAdapterPosition
		adapter.moveItem(oldPos, newPos)
		return true
	}

	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

	override fun isLongPressDragEnabled() = false
}