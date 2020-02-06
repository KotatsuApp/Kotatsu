package org.koitharu.kotatsu.ui.search

import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.ui.main.list.MangaListFragment
import org.koitharu.kotatsu.utils.ext.withArgs

class SearchFragment : MangaListFragment<Unit>() {

	private val presenter by moxyPresenter(factory = ::SearchPresenter)

	private val query by stringArg(ARG_QUERY)

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(query.orEmpty(), offset)
	}

	override fun getTitle(): CharSequence? {
		return query
	}

	companion object {

		private const val ARG_QUERY = "query"

		fun newInstance(query: String) = SearchFragment().withArgs(1) {
			putString(ARG_QUERY, query)
		}
	}
}