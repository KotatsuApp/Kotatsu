package org.koitharu.kotatsu.settings.sources.catalog

import android.app.Activity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.util.toTitleCase

class SourcesCatalogMenuProvider(
	private val activity: Activity,
	private val viewModel: SourcesCatalogViewModel,
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

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_locales -> {
			showLocalesMenu()
			true
		}

		else -> false
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		(item.actionView as SearchView).setQuery("", false)
		return true
	}

	override fun onQueryTextSubmit(query: String?): Boolean = false

	override fun onQueryTextChange(newText: String?): Boolean {
		viewModel.performSearch(newText.orEmpty())
		return true
	}

	private fun showLocalesMenu() {
		val locales = viewModel.locales
		val anchor: View = (activity as AppBarOwner).appBar.let {
			it.findViewById<View?>(R.id.toolbar) ?: it
		}
		val menu = PopupMenu(activity, anchor)
		for ((i, lc) in locales.withIndex()) {
			val title = lc?.getDisplayLanguage(lc)?.toTitleCase(lc) ?: activity.getString(R.string.various_languages)
			menu.menu.add(Menu.NONE, Menu.NONE, i, title)
		}
		menu.setOnMenuItemClickListener {
			viewModel.setLocale(locales.getOrNull(it.order)?.language)
			true
		}
		menu.show()
	}
}
