package org.koitharu.kotatsu.remotelist.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.list.ui.filter.FilterSheetFragment
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.settings.SettingsActivity

@AndroidEntryPoint
class RemoteListFragment : MangaListFragment() {

	public override val viewModel by viewModels<RemoteListViewModel>()

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		addMenuProvider(RemoteListMenuProvider())
	}

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onFilterClick(view: View?) {
		FilterSheetFragment.show(childFragmentManager)
	}

	override fun onEmptyActionClick() {
		viewModel.resetFilter()
	}

	private inner class RemoteListMenuProvider :
		MenuProvider,
		SearchView.OnQueryTextListener,
		MenuItem.OnActionExpandListener {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.opt_list_remote, menu)
			val searchMenuItem = menu.findItem(R.id.action_search)
			searchMenuItem.setOnActionExpandListener(this)
			val searchView = searchMenuItem.actionView as SearchView
			searchView.setOnQueryTextListener(this)
			searchView.setIconifiedByDefault(false)
			searchView.queryHint = searchMenuItem.title
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
			R.id.action_source_settings -> {
				startActivity(SettingsActivity.newSourceSettingsIntent(requireContext(), viewModel.source))
				true
			}

			R.id.action_filter -> {
				onFilterClick(null)
				true
			}

			else -> false
		}

		override fun onQueryTextSubmit(query: String?): Boolean {
			if (query.isNullOrEmpty()) {
				return false
			}
			val intent = SearchActivity.newIntent(
				context = this@RemoteListFragment.context ?: return false,
				source = viewModel.source,
				query = query,
			)
			startActivity(intent)
			return true
		}

		override fun onQueryTextChange(newText: String?): Boolean = false

		override fun onMenuItemActionExpand(item: MenuItem): Boolean {
			(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
			return true
		}

		override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
			val searchView = (item.actionView as? SearchView) ?: return false
			searchView.setQuery("", false)
			return true
		}
	}

	companion object {

		const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putSerializable(ARG_SOURCE, provider)
		}
	}
}
