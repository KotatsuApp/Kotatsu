package org.koitharu.kotatsu.search.ui

import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ext.parcelableArgument
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class SearchFragment : MangaListFragment() {

	override val viewModel by viewModel<SearchViewModel> {
		parametersOf(source)
	}

	private val query by stringArgument(ARG_QUERY)
	private val source by parcelableArgument<MangaSource>(ARG_SOURCE)

	override fun onRequestMoreItems(offset: Int) {
		viewModel.loadList(query.orEmpty(), offset)
	}

	override fun getTitle(): CharSequence? {
		return query
	}

	companion object {

		private const val ARG_QUERY = "query"
		private const val ARG_SOURCE = "source"

		fun newInstance(source: MangaSource, query: String) = SearchFragment().withArgs(2) {
			putParcelable(ARG_SOURCE, source)
			putString(ARG_QUERY, query)
		}
	}
}