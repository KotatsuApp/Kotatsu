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
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.getCauseUrl
import org.koitharu.kotatsu.core.util.ext.isHttpUrl
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.search.domain.SearchKind

@AndroidEntryPoint
class RemoteListFragment : MangaListFragment(), FilterCoordinator.Owner, View.OnClickListener {

    override val viewModel by viewModels<RemoteListViewModel>()

    override val filterCoordinator: FilterCoordinator
        get() = viewModel.filterCoordinator

    override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
        super.onViewBindingCreated(binding, savedInstanceState)
        addMenuProvider(RemoteListMenuProvider())
        addMenuProvider(MangaSearchMenuProvider(filterCoordinator, viewModel))
        viewModel.isRandomLoading.observe(viewLifecycleOwner, MenuInvalidator(requireActivity()))
        viewModel.onOpenManga.observeEvent(viewLifecycleOwner) { router.openDetails(it) }
        viewModel.onSourceBroken.observeEvent(viewLifecycleOwner) { showSourceBrokenWarning() }
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
        router.showFilterSheet()
    }

    override fun onEmptyActionClick() {
        if (filterCoordinator.isFilterApplied) {
            filterCoordinator.reset()
        } else {
            openInBrowser(null) // should never be called
        }
    }

    override fun onFooterButtonClick() {
        val filter = filterCoordinator.snapshot().listFilter
        when {
            !filter.query.isNullOrEmpty() -> router.openSearch(filter.query.orEmpty(), SearchKind.SIMPLE)
            !filter.author.isNullOrEmpty() -> router.openSearch(filter.author.orEmpty(), SearchKind.AUTHOR)
            filter.tags.size == 1 -> router.openSearch(filter.tags.singleOrNull()?.title.orEmpty(), SearchKind.TAG)
        }
    }

    override fun onSecondaryErrorActionClick(error: Throwable) {
        openInBrowser(error.getCauseUrl())
    }

    override fun onClick(v: View?) = Unit // from Snackbar, do nothing

    private fun openInBrowser(url: String?) {
        if (url?.isHttpUrl() == true) {
            router.openBrowser(
                url = url,
                source = viewModel.source,
                title = viewModel.source.getTitle(requireContext()),
            )
        } else {
            Snackbar.make(requireViewBinding().recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun showSourceBrokenWarning() {
        val snackbar = Snackbar.make(
            viewBinding?.recyclerView ?: return,
            R.string.source_broken_warning,
            Snackbar.LENGTH_INDEFINITE,
        )
        snackbar.setAction(R.string.got_it, this)
        snackbar.show()
    }

    private inner class RemoteListMenuProvider : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.opt_list_remote, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            R.id.action_source_settings -> {
                router.openSourceSettings(viewModel.source)
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
