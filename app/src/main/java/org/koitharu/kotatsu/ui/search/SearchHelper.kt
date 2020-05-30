package org.koitharu.kotatsu.ui.search

import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.search.global.GlobalSearchActivity
import org.koitharu.kotatsu.utils.ext.safe

object SearchHelper {

	@JvmStatic
	fun setupSearchView(menuItem: MenuItem) {
		val view = menuItem.actionView as? SearchView ?: return
		val context = view.context
		view.queryHint = context.getString(R.string.search_manga)
		view.suggestionsAdapter = MangaSuggestionsProvider.getSuggestionAdapter(context)
		view.setOnQueryTextListener(QueryListener(context))
		view.setOnSuggestionListener(SuggestionListener(view))
	}

	private class QueryListener(private val context: Context) :
		SearchView.OnQueryTextListener {

		override fun onQueryTextSubmit(query: String?): Boolean {
			return if (!query.isNullOrBlank()) {
				context.startActivity(GlobalSearchActivity.newIntent(context, query.trim()))
				MangaSuggestionsProvider.saveQuery(context, query)
				true
			} else false
		}

		override fun onQueryTextChange(newText: String?) = false
	}

	class SuggestionListener(private val view: SearchView) :
		SearchView.OnSuggestionListener {

		override fun onSuggestionSelect(position: Int) = false

		override fun onSuggestionClick(position: Int): Boolean {
			val query = safe {
				val c = view.suggestionsAdapter.getItem(position) as? Cursor
				c?.getString(c.getColumnIndex(SearchManager.SUGGEST_COLUMN_QUERY))
			} ?: return false
			view.setQuery(query, true)
			return true
		}
	}
}