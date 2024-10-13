package org.koitharu.kotatsu.remotelist.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.getCauseUrl
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.filter.ui.sheet.FilterSheetFragment
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.SettingsActivity

@AndroidEntryPoint
class RemoteListFragment : MangaListFragment(), FilterCoordinator.Owner {

	override val viewModel by viewModels<RemoteListViewModel>()

	override val filterCoordinator: FilterCoordinator
		get() = viewModel.filterCoordinator

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		addMenuProvider(RemoteListMenuProvider())
		addMenuProvider(MangaSearchMenuProvider(filterCoordinator, viewModel))
		viewModel.isRandomLoading.observe(viewLifecycleOwner, MenuInvalidator(requireActivity()))
		viewModel.onOpenManga.observeEvent(viewLifecycleOwner) {
			startActivity(DetailsActivity.newIntent(binding.root.context, it))
		}
		filterCoordinator.observe().distinctUntilChangedBy { it.listFilter.isEmpty() }
			.drop(1)
			.observe(viewLifecycleOwner) {
				activity?.invalidateMenu()
			}
	}

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onCreateActionMode(
		controller: ListSelectionController,
		menuInflater: MenuInflater,
		menu: Menu
	): Boolean {
		menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, menuInflater, menu)
	}

	override fun onFilterClick(view: View?) {
		FilterSheetFragment.show(getChildFragmentManager())
	}

	override fun onEmptyActionClick() {
		if (filterCoordinator.isFilterApplied) {
			filterCoordinator.reset()
		} else {
			openInBrowser(null) // should never be called
		}
	}

	override fun onSecondaryErrorActionClick(error: Throwable) {
		openInBrowser(error.getCauseUrl())
	}

	private fun openInBrowser(url: String?) {
		if (url.isNullOrEmpty()) {
			Snackbar.make(requireViewBinding().recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT)
				.show()
		} else {
			startActivity(
				BrowserActivity.newIntent(
					requireContext(),
					url,
					viewModel.source,
					viewModel.source.getTitle(requireContext()),
				),
			)
		}
	}

	private inner class RemoteListMenuProvider : MenuProvider {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.opt_list_remote, menu)
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
			R.id.action_source_settings -> {
				startActivity(SettingsActivity.newSourceSettingsIntent(requireContext(), viewModel.source))
				true
			}

			R.id.action_random -> {
				viewModel.openRandom()
				true
			}

			R.id.action_filter -> {
				onFilterClick(null)
				true
			}

			R.id.action_filter_reset -> {
				filterCoordinator.reset()
				true
			}

			else -> false
		}

		override fun onPrepareMenu(menu: Menu) {
			super.onPrepareMenu(menu)
			menu.findItem(R.id.action_random)?.isEnabled = !viewModel.isRandomLoading.value
			menu.findItem(R.id.action_filter_reset)?.isVisible = filterCoordinator.isFilterApplied
		}
	}

	companion object {

		const val ARG_SOURCE = "provider"

		fun newInstance(source: MangaSource) = RemoteListFragment().withArgs(1) {
			putString(ARG_SOURCE, source.name)
		}
	}
}
