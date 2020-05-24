package org.koitharu.kotatsu.ui.search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.list.MangaListSheet
import org.koitharu.kotatsu.utils.ext.withArgs

class MangaSearchSheet : MangaListSheet<Unit>() {

	private val presenter by moxyPresenter(factory = ::SearchPresenter)

	private lateinit var source: MangaSource
	private lateinit var query: String

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		source = requireArguments().getParcelable(ARG_SOURCE)!!
		query = requireArguments().getString(ARG_QUERY).orEmpty()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setTitle(query)
		setSubtitle(getString(R.string.search_results_on_s, source.title))
	}

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(source, query, offset)
	}

	companion object {

		private const val ARG_SOURCE = "source"
		private const val ARG_QUERY = "query"

		private const val TAG = "MangaSearchSheet"

		fun  show(fm: FragmentManager, source: MangaSource, query: String) {
			MangaSearchSheet().withArgs(2) {
				putParcelable(ARG_SOURCE, source)
				putString(ARG_QUERY, query)
			}.show(fm, TAG)
		}
	}
}