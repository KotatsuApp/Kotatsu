package org.koitharu.kotatsu.ui.search

import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.list.MangaListFragment
import org.koitharu.kotatsu.utils.ext.parcelableArgument
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class SearchFragment : MangaListFragment<Unit>() {

	private val presenter by moxyPresenter(factory = ::SearchPresenter)

	private val query by stringArgument(ARG_QUERY)
	private val source by parcelableArgument<MangaSource>(ARG_SOURCE)

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(source, query.orEmpty(), offset)
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