package org.koitharu.kotatsu.search.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.list.ui.MangaListSheet
import org.koitharu.kotatsu.utils.ext.parcelableArgument
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class MangaSearchSheet : MangaListSheet() {

	override val viewModel by viewModel<SearchViewModel> {
		parametersOf(source, query)
	}

	private val query by stringArgument(ARG_QUERY)
	private val source by parcelableArgument<MangaSource>(ARG_SOURCE)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setTitle(query.orEmpty())
		setSubtitle(getString(R.string.search_results_on_s, source.title))
	}

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	companion object {

		private const val ARG_SOURCE = "source"
		private const val ARG_QUERY = "query"

		private const val TAG = "MangaSearchSheet"

		fun show(fm: FragmentManager, source: MangaSource, query: String) {
			MangaSearchSheet().withArgs(2) {
				putParcelable(ARG_SOURCE, source)
				putString(ARG_QUERY, query)
			}.show(fm, TAG)
		}
	}
}