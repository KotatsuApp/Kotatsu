package org.koitharu.kotatsu.details.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import java.lang.ref.WeakReference

class ChaptersMenuProvider(
	private val viewModel: DetailsViewModel,
	private val bottomSheetMediator: ChaptersBottomSheetMediator?,
) : OnBackPressedCallback(false), MenuProvider, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

	private var searchItemRef: WeakReference<MenuItem>? = null

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_chapters, menu)
		val searchMenuItem = menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
		searchItemRef = WeakReference(searchMenuItem)
	}

	override fun onPrepareMenu(menu: Menu) {
		menu.findItem(R.id.action_search)?.isVisible = viewModel.isChaptersEmpty.value == false
		menu.findItem(R.id.action_reversed)?.isChecked = viewModel.isChaptersReversed.value == true
		menu.findItem(R.id.action_grid_view)?.isChecked = viewModel.isChaptersInGridView.value == true
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_reversed -> {
			viewModel.setChaptersReversed(!menuItem.isChecked)
			true
		}
		R.id.action_grid_view-> {
			viewModel.setChaptersInGridView(!menuItem.isChecked)
			true
		}

		else -> false
	}

	override fun handleOnBackPressed() {
		searchItemRef?.get()?.collapseActionView()
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		bottomSheetMediator?.lock()
		isEnabled = true
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		isEnabled = false
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
