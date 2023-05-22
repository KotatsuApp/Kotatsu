package org.koitharu.kotatsu.search.ui.suggestion

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.search.ui.suggestion.adapter.SEARCH_SUGGESTION_ITEM_TYPE_QUERY
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem

class SearchSuggestionItemCallback(
	private val listener: SuggestionItemListener,
) : ItemTouchHelper.Callback() {

	private val movementFlags = makeMovementFlags(
		0,
		ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
	)

	override fun getMovementFlags(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
	): Int = if (viewHolder.itemViewType == SEARCH_SUGGESTION_ITEM_TYPE_QUERY) {
		movementFlags
	} else {
		0
	}

	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder,
	): Boolean = false

	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
		val item = viewHolder.getItem(SearchSuggestionItem.RecentQuery::class.java) ?: return
		listener.onRemoveQuery(item.query)
	}

	interface SuggestionItemListener {

		fun onRemoveQuery(query: String)
	}
}
