package org.koitharu.kotatsu.search.ui

import android.view.Menu
import androidx.appcompat.view.ActionMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.assistedViewModels
import org.koitharu.kotatsu.utils.ext.serializableArgument
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs

@AndroidEntryPoint
class SearchFragment : MangaListFragment() {

	@Inject
	lateinit var viewModelFactory: SearchViewModel.Factory

	override val viewModel by assistedViewModels {
		viewModelFactory.create(source, query.orEmpty())
	}

	private val query by stringArgument(ARG_QUERY)
	private val source by serializableArgument<MangaSource>(ARG_SOURCE)

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	companion object {

		private const val ARG_QUERY = "query"
		private const val ARG_SOURCE = "source"

		fun newInstance(source: MangaSource, query: String) = SearchFragment().withArgs(2) {
			putSerializable(ARG_SOURCE, source)
			putString(ARG_QUERY, query)
		}
	}
}
