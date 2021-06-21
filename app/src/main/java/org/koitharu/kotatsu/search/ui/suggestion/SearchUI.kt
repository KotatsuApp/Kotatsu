package org.koitharu.kotatsu.search.ui.suggestion

import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import org.koitharu.kotatsu.R

class SearchUI(
	private val searchView: SearchView,
	listener: SearchSuggestionListener,
	hint: String? = null,
) {

	init {
		val context = searchView.context
		searchView.queryHint = hint ?: context.getString(R.string.search_manga)
		searchView.setOnQueryTextListener(QueryListener(listener))
	}

	var query: String
		get() = searchView.query.toString()
		set(value) {
			searchView.setQuery(value, false)
		}

	private class QueryListener(
		private val listener: SearchSuggestionListener,
	) : SearchView.OnQueryTextListener {

		override fun onQueryTextSubmit(query: String?): Boolean {
			return if (!query.isNullOrBlank()) {
				listener.onQueryClick(query.trim(), submit = true)
				true
			} else false
		}

		override fun onQueryTextChange(newText: String?): Boolean {
			listener.onQueryChanged(newText?.trim().orEmpty())
			return true
		}
	}

	companion object {

		fun from(
			searchView: SearchView,
			listener: SearchSuggestionListener,
		): SearchUI = SearchUI(searchView, listener)
	}
}