package org.koitharu.kotatsu.remotelist.ui

import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ext.parcelableArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class RemoteListFragment : MangaListFragment() {

	override val viewModel by viewModel<RemoteListViewModel>(mode = LazyThreadSafetyMode.NONE) {
		parametersOf(source)
	}

	private val source by parcelableArgument<MangaSource>(ARG_SOURCE)

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun getTitle(): CharSequence {
		return source.title
	}

	override fun onFilterChanged(filter: MangaFilter) {
		viewModel.applyFilter(filter)
		super.onFilterChanged(filter)
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putParcelable(ARG_SOURCE, provider)
		}
	}
}