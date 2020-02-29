package org.koitharu.kotatsu.ui.main.list.remote

import android.view.Menu
import android.view.MenuInflater
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.main.list.MangaListFragment
import org.koitharu.kotatsu.ui.search.SearchHelper
import org.koitharu.kotatsu.utils.ext.withArgs

class RemoteListFragment : MangaListFragment<Unit>() {

	private val presenter by moxyPresenter(factory = ::RemoteListPresenter)

	private val source by arg<MangaSource>(ARG_SOURCE)

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(source, offset)
	}

	override fun getTitle(): CharSequence? {
		return source.title
	}

	override fun onFilterChanged(filter: MangaFilter) {
		presenter.applyFilter(source, filter)
		super.onFilterChanged(filter)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_remote, menu)
		menu.findItem(R.id.action_search)?.let { menuItem ->
			SearchHelper.setupSearchView(menuItem, source)
		}
		super.onCreateOptionsMenu(menu, inflater)
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putParcelable(ARG_SOURCE, provider)
		}
	}
}