package org.koitharu.kotatsu.settings.search

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R

class SettingsSearchMenuProvider(
	private val viewModel: SettingsSearchViewModel,
) : MenuProvider, MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_search, menu)
		val menuItem = menu.findItem(R.id.action_search)
		menuItem.setOnActionExpandListener(this)
		val searchView = menuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.queryHint = menuItem.title
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		val currentQuery = viewModel.currentQuery
		if (currentQuery.isNotEmpty()) {
			val menuItem = menu.findItem(R.id.action_search)
			menuItem.expandActionView()
			val searchView = menuItem.actionView as SearchView
			searchView.setQuery(currentQuery, false)
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

	override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		viewModel.discardSearch()
		return true
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean {
		viewModel.onQueryChanged(newText.orEmpty())
		return true
	}
}
