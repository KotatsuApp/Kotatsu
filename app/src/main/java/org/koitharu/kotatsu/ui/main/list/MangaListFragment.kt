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
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.common.list.PaginationScrollListener
import org.koitharu.kotatsu.ui.common.list.SpacingItemDecoration
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.clearItemDecorations
import org.koitharu.kotatsu.utils.ext.firstItem
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.hasItems

abstract class MangaListFragment <E> : BaseFragment(R.layout.fragment_list), MangaListView<E>,
	PaginationScrollListener.Callback, OnRecyclerItemClickListener<Manga> {

	private lateinit var adapter: MangaListAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaListAdapter(this)
		initListMode(settings.listMode)
		recyclerView.adapter = adapter
		recyclerView.addOnScrollListener(PaginationScrollListener(4, this))
		swipeRefreshLayout.setOnRefreshListener {
			onRequestMoreItems(0)
		}
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		if (!recyclerView.hasItems) {
			onRequestMoreItems(0)
		}
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

	override fun onItemClick(item: Manga, position: Int, view: View) {
		startActivity(MangaDetailsActivity.newIntent(context ?: return, item))
	}

	override fun onListChanged(list: List<Manga>) {
		adapter.replaceData(list)
		if (list.isEmpty()) {
			setUpEmptyListHolder()
			layout_holder.isVisible = true
		} else {
			layout_holder.isVisible = false
		}
	}

	override fun onListAppended(list: List<Manga>) {
		adapter.appendData(list)
	}

	override fun onError(e: Exception) {
		if (recyclerView.hasItems) {
			Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
		} else {
			textView_holder.text = e.getDisplayMessage(resources)
			textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_error_large, 0, 0)
			layout_holder.isVisible = true
		}
	}

	override fun onLoadingChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems
		swipeRefreshLayout.isRefreshing = isLoading && hasItems
		swipeRefreshLayout.isEnabled = !progressBar.isVisible
		if (isLoading) {
			layout_holder.isVisible = false
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		when(key) {
			getString(R.string.key_list_mode) -> initListMode(settings.listMode)
		}
	}

	protected open fun setUpEmptyListHolder() {
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
		textView_holder.setText(R.string.nothing_found)
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
			ListMode.LIST -> DividerItemDecoration(ctx, RecyclerView.VERTICAL)
			ListMode.DETAILED_LIST,
			ListMode.GRID -> SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing))
		})
		adapter.notifyDataSetChanged()
		recyclerView.firstItem = position
	}
}