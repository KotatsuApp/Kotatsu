package org.koitharu.kotatsu.utils

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import org.koitharu.kotatsu.ui.search.SearchActivity

object SearchHelper {

	@JvmStatic
	fun setupSearchView(menuItem: MenuItem) {
		val view = menuItem.actionView as? SearchView ?: return
		val context = view.context
		val searchManager = context.applicationContext.getSystemService(Context.SEARCH_SERVICE) as SearchManager
		val info = searchManager.getSearchableInfo(ComponentName(context, SearchActivity::class.java))
		view.setSearchableInfo(info)
	}
}