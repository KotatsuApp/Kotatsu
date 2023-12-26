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
import org.koitharu.kotatsu.core.util.LocaleComparator
import org.koitharu.kotatsu.core.util.ext.getDisplayName
import org.koitharu.kotatsu.core.util.ext.toLocale
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

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_locales -> {
			showLocalesMenu()
			true
		}

		else -> false
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
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

	private fun showLocalesMenu() {
		val locales = viewModel.locales.mapTo(ArrayList(viewModel.locales.size)) {
			it to it?.toLocale()
		}
		locales.sortWith(compareBy(nullsFirst(LocaleComparator())) { it.second })

		val anchor: View = (activity as AppBarOwner).appBar.let {
			it.findViewById<View?>(R.id.toolbar) ?: it
		}
		val menu = PopupMenu(activity, anchor)
		for ((i, lc) in locales.withIndex()) {
			menu.menu.add(Menu.NONE, Menu.NONE, i, lc.second.getDisplayName(activity))
		}
		menu.setOnMenuItemClickListener {
			viewModel.setLocale(locales.getOrNull(it.order)?.first)
			true
		}
		menu.show()
	}
}
