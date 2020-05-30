package org.koitharu.kotatsu.ui.search.global

import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.ui.list.MangaListFragment
import org.koitharu.kotatsu.utils.ext.withArgs


class GlobalSearchFragment: MangaListFragment<Unit>() {

	private val presenter by moxyPresenter(factory = ::GlobalSearchPresenter)

	private val query by stringArg(ARG_QUERY)

	override fun onRequestMoreItems(offset: Int) {
		if (offset == 0) {
			presenter.startSearch(query.orEmpty())
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