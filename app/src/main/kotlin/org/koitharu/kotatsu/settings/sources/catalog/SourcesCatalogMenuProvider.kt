package org.koitharu.kotatsu.settings.sources.catalog

import android.app.Activity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner

class SourcesCatalogMenuProvider(
	private val activity: Activity,
	private val viewModel: SourcesCatalogViewModel,
	private val expandListener: MenuItem.OnActionExpandListener,
) : MenuProvider,
	MenuItem.OnActionExpandListener,
	SearchView.OnQueryTextListener {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_sources_catalog, menu)
		val searchMenuItem = menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		(activity as? AppBarOwner)?.appBar?.setExpanded(true, true)
		return expandListener.onMenuItemActionExpand(item)
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		(item.actionView as SearchView).setQuery("", false)
		return expandListener.onMenuItemActionCollapse(item)
	}

	override fun onQueryTextSubmit(query: String?): Boolean = false

	override fun onQueryTextChange(newText: String?): Boolean {
		viewModel.performSearch(newText?.trim().orEmpty())
		return true
	}
}
