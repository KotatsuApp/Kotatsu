package org.koitharu.kotatsu.ui.main.list

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.PaginationScrollListener
import org.koitharu.kotatsu.ui.common.list.SpacingItemDecoration
import org.koitharu.kotatsu.ui.main.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.*

class MangaListFragment : BaseFragment(R.layout.fragment_list), MangaListView,
	PaginationScrollListener.Callback {

	private val presenter by moxyPresenter(factory = ::MangaListPresenter)

	private val source by arg<MangaSource>(ARG_SOURCE)

	private lateinit var adapter: MangaListAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaListAdapter {
			startActivity(MangaDetailsActivity.newIntent(context ?: return@MangaListAdapter, it))
		}
		initListMode(settings.listMode)
		recyclerView.adapter = adapter
		recyclerView.addOnScrollListener(PaginationScrollListener(4, this))
		swipeRefreshLayout.setOnRefreshListener {
			presenter.loadList(source, 0)
		}
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		presenter.loadList(source, 0)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_list, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
		R.id.action_list_mode -> {
			ListModeSelectDialog.show(childFragmentManager)
			true
		}
		else -> super.onOptionsItemSelected(item)
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

	override fun onError(e: Exception) {
		if (recyclerView.hasItems) {
			Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
		}
	}

	override fun onLoadingChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems
		swipeRefreshLayout.isRefreshing = isLoading && hasItems
		swipeRefreshLayout.isEnabled = !progressBar.isVisible
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		when(key) {
			getString(R.string.key_list_mode) -> initListMode(settings.listMode)
		}
	}

	private fun initListMode(mode: ListMode) {
		val ctx = context ?: return
		val position = recyclerView.firstItem
		recyclerView.adapter = null
		recyclerView.layoutManager = null
		recyclerView.clearItemDecorations()
		adapter.listMode = mode
		recyclerView.layoutManager = when(mode) {
			ListMode.GRID -> GridLayoutManager(ctx, 3)
			else -> LinearLayoutManager(ctx)
		}
		recyclerView.adapter = adapter
		recyclerView.addItemDecoration(when(mode) {
			ListMode.DETAILED_LIST,
			ListMode.LIST -> DividerItemDecoration(ctx, RecyclerView.VERTICAL)
			ListMode.GRID -> SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing))
		})
		adapter.notifyDataSetChanged()
		recyclerView.firstItem = position
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = MangaListFragment().withArgs(1) {
			putParcelable(ARG_SOURCE, provider)
		}
	}
}