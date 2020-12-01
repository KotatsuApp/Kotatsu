package org.koitharu.kotatsu.search.ui.global

import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs


class GlobalSearchFragment : MangaListFragment() {

	override val viewModel by viewModel<GlobalSearchViewModel> {
		parametersOf(query)
	}

	private val query by stringArgument(ARG_QUERY)

	override fun onRefresh() {
		super.onRefresh()
		viewModel.onRefresh()
	}

	override fun onScrolledToEnd() = Unit

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