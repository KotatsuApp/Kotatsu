package org.koitharu.kotatsu.search.ui.global

import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs


class GlobalSearchFragment : MangaListFragment() {

	override val viewModel by viewModel<GlobalSearchViewModel>()

	private val query by stringArgument(ARG_QUERY)

	override fun onRequestMoreItems(offset: Int) {
		if (offset == 0) {
			viewModel.startSearch(query.orEmpty())
		}
	}

	override fun getTitle(): CharSequence? {
		return query
	}

	companion object {

		private const val ARG_QUERY = "query"

		fun newInstance(query: String) = GlobalSearchFragment().withArgs(1) {
			putString(ARG_QUERY, query)
		}
	}
}