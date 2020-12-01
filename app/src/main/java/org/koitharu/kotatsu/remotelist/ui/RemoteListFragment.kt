package org.koitharu.kotatsu.remotelist.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.utils.ext.parcelableArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class RemoteListFragment : MangaListFragment() {

	override val viewModel by viewModel<RemoteListViewModel> {
		parametersOf(source)
	}

	private val source by parcelableArgument<MangaSource>(ARG_SOURCE)

	override fun onRefresh() {
		super.onRefresh()
		viewModel.loadList(append = false)
	}

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	override fun getTitle(): CharSequence? {
		return source.title
	}

	override fun onFilterChanged(filter: MangaFilter) {
		viewModel.applyFilter(filter)
		super.onFilterChanged(filter)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_remote, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_search_internal -> {
			context?.startActivity(SearchActivity.newIntent(requireContext(), source, null))
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putParcelable(ARG_SOURCE, provider)
		}
	}
}