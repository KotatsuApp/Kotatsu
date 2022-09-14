package org.koitharu.kotatsu.details.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R

class ChaptersMenuProvider(
	private val viewModel: DetailsViewModel,
	private val bottomSheetMediator: ChaptersBottomSheetMediator?,
) : MenuProvider, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_chapters, menu)
		val searchMenuItem = menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
	}

	override fun onPrepareMenu(menu: Menu) {
		menu.findItem(R.id.action_reversed)?.isChecked = viewModel.isChaptersReversed.value == true
		menu.findItem(R.id.action_search)?.isVisible = viewModel.isChaptersEmpty.value == false
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_reversed -> {
			viewModel.setChaptersReversed(!menuItem.isChecked)
			true
		}
		else -> false
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		bottomSheetMediator?.lock()
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		(item.actionView as? SearchView)?.setQuery("", false)
		viewModel.performChapterSearch(null)
		bottomSheetMediator?.unlock()
		return true
	}

	override fun onQueryTextSubmit(query: String?): Boolean = false

	override fun onQueryTextChange(newText: String?): Boolean {
		viewModel.performChapterSearch(newText)
		return true
	}
}
