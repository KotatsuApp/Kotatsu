package org.koitharu.kotatsu.reader.ui

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.reader.domain.MangaTranslationPreference

class TranslationPreferenceItemTouchCallback(
	private val adapter: TranslationPreferencesAdapter,
	private val onOrderChanged: (List<MangaTranslationPreference>) -> Unit
) : ItemTouchHelper.SimpleCallback(
	ItemTouchHelper.UP or ItemTouchHelper.DOWN,
	0 // No swipe functionality
) {

	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder
	): Boolean {
		val fromPosition = viewHolder.adapterPosition
		val toPosition = target.adapterPosition
		adapter.moveItem(fromPosition, toPosition)
		return true
	}

	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
		// No swipe functionality
	}

	override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
		super.clearView(recyclerView, viewHolder)
		// Notify about the new order when dragging is complete
		onOrderChanged(adapter.currentList)
	}

	override fun isLongPressDragEnabled(): Boolean = false // We handle drag from the drag handle

	override fun isItemViewSwipeEnabled(): Boolean = false
}