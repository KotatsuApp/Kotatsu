package org.koitharu.kotatsu.utils

import android.view.MenuItem
import androidx.appcompat.widget.SearchView

object SearchHelper {

	@JvmStatic
	fun setupSearchView(menuItem: MenuItem) {
		val view = menuItem.actionView as? SearchView ?: return
		//TODO
	}
}