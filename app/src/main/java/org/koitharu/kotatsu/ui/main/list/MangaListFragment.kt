package org.koitharu.kotatsu.ui.main.list

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.PaginationScrollListener
import org.koitharu.kotatsu.ui.common.list.SpacingItemDecoration
import org.koitharu.kotatsu.utils.ext.hasItems
import org.koitharu.kotatsu.utils.ext.withArgs

class MangaListFragment : BaseFragment(R.layout.fragment_list), MangaListView,
	PaginationScrollListener.Callback {

	private val presenter by moxyPresenter(factory = ::MangaListPresenter)

	private val source by arg<MangaSource>(ARG_SOURCE)

	private lateinit var adapter: MangaListAdapter

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaListAdapter {

		}
		recyclerView.addItemDecoration(SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing)))
		recyclerView.adapter = adapter
		recyclerView.addOnScrollListener(PaginationScrollListener(4, this))
		swipeRefreshLayout.setOnRefreshListener {
			presenter.loadList(source, 0)
		}
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		presenter.loadList(source, 0)
	}

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(source, offset)
	}

	override fun onListChanged(list: List<Manga>) {
		adapter.replaceData(list)
	}

	override fun onListAppended(list: List<Manga>) {
		adapter.appendData(list)
	}

	override fun onLoadingChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems
		swipeRefreshLayout.isRefreshing = isLoading && hasItems
		swipeRefreshLayout.isEnabled = !progressBar.isVisible
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = MangaListFragment().withArgs(1) {
			putParcelable(ARG_SOURCE, provider)
		}
	}
}