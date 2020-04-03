package org.koitharu.kotatsu.ui.main.list

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.MvpDelegate
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.common.list.PaginationScrollListener
import org.koitharu.kotatsu.ui.common.list.decor.ItemTypeDividerDecoration
import org.koitharu.kotatsu.ui.common.list.decor.SectionItemDecoration
import org.koitharu.kotatsu.ui.common.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.ui.main.list.filter.FilterAdapter
import org.koitharu.kotatsu.ui.main.list.filter.OnFilterChangedListener
import org.koitharu.kotatsu.utils.UiUtils
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListFragment<E> : BaseFragment(R.layout.fragment_list), MangaListView<E>,
	PaginationScrollListener.Callback, OnRecyclerItemClickListener<Manga>,
	SharedPreferences.OnSharedPreferenceChangeListener, OnFilterChangedListener,
	SectionItemDecoration.Callback, SwipeRefreshLayout.OnRefreshListener {

	private val settings by inject<AppSettings>()

	private var adapter: MangaListAdapter? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
		adapter = MangaListAdapter(this)
		recyclerView.setHasFixedSize(true)
		initListMode(settings.listMode)
		recyclerView.adapter = adapter
		recyclerView.addOnScrollListener(PaginationScrollListener(4, this))
		swipeRefreshLayout.setOnRefreshListener(this)
		recyclerView_filter.setHasFixedSize(true)
		recyclerView_filter.addItemDecoration(ItemTypeDividerDecoration(view.context))
		recyclerView_filter.addItemDecoration(SectionItemDecoration(false, this))
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		adapter = null
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		if (savedInstanceState?.containsKey(MvpDelegate.MOXY_DELEGATE_TAGS_KEY) != true) {
			onRequestMoreItems(0)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_list, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_list_mode -> {
			ListModeSelectDialog.show(childFragmentManager)
			true
		}
		R.id.action_filter -> {
			drawer?.toggleDrawer(GravityCompat.END)
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		menu.findItem(R.id.action_filter).isVisible = drawer != null &&
				drawer?.getDrawerLockMode(GravityCompat.END) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED
		super.onPrepareOptionsMenu(menu)
	}

	override fun onItemClick(item: Manga, position: Int, view: View) {
		startActivity(MangaDetailsActivity.newIntent(context ?: return, item))
	}

	override fun onItemLongClick(item: Manga, position: Int, view: View): Boolean {
		val menu = PopupMenu(context ?: return false, view)
		onCreatePopupMenu(menu.menuInflater, menu.menu, item)
		return if (menu.menu.hasVisibleItems()) {
			menu.setOnMenuItemClickListener {
				onPopupMenuItemSelected(it, item)
			}
			menu.gravity = GravityCompat.END or Gravity.TOP
			menu.show()
			true
		} else {
			false
		}
	}

	final override fun onRefresh() {
		onRequestMoreItems(0)
	}

	override fun onListChanged(list: List<Manga>) {
		adapter?.replaceData(list)
		if (list.isEmpty()) {
			setUpEmptyListHolder()
			layout_holder.isVisible = true
		} else {
			layout_holder.isVisible = false
		}
		recyclerView.callOnScrollListeners()
	}

	override fun onListAppended(list: List<Manga>) {
		adapter?.appendData(list)
		recyclerView.callOnScrollListeners()
	}

	@CallSuper
	override fun onItemRemoved(item: Manga) {
		adapter?.let {
			it.removeItem(item)
			if (it.itemCount == 0) {
				setUpEmptyListHolder()
				layout_holder.isVisible = true
			} else {
				layout_holder.isVisible = false
			}
		}
	}

	override fun onListError(e: Throwable) {
		if (recyclerView.hasItems) {
			Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT)
				.show()
		} else {
			textView_holder.text = e.getDisplayMessage(resources)
			textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(
				0,
				R.drawable.ic_error_large,
				0,
				0
			)
			layout_holder.isVisible = true
		}
	}

	override fun onError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	@CallSuper
	override fun onLoadingStateChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems
		swipeRefreshLayout.isRefreshing = isLoading && hasItems
		swipeRefreshLayout.isEnabled = !progressBar.isVisible
		if (isLoading) {
			layout_holder.isVisible = false
		}
	}

	@CallSuper
	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		if (context == null) {
			return
		}
		when (key) {
			getString(R.string.key_list_mode) -> initListMode(settings.listMode)
			getString(R.string.key_grid_size) -> UiUtils.SpanCountResolver.update(recyclerView)
		}
	}

	override fun onInitFilter(
		sortOrders: List<SortOrder>,
		tags: List<MangaTag>,
		currentFilter: MangaFilter?
	) {
		recyclerView_filter.adapter = FilterAdapter(sortOrders, tags, currentFilter, this)
		drawer?.setDrawerLockMode(
			if (sortOrders.isEmpty() && tags.isEmpty()) {
				DrawerLayout.LOCK_MODE_LOCKED_CLOSED
			} else {
				DrawerLayout.LOCK_MODE_UNLOCKED
			}
		) ?: divider_filter?.let {
			it.isGone = sortOrders.isEmpty() && tags.isEmpty()
			recyclerView_filter.isVisible = it.isVisible
		}
		activity?.invalidateOptionsMenu()
	}

	@CallSuper
	override fun onFilterChanged(filter: MangaFilter) {
		drawer?.closeDrawers()
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
		recyclerView.removeOnLayoutChangeListener(UiUtils.SpanCountResolver)
		adapter?.listMode = mode
		recyclerView.layoutManager = when (mode) {
			ListMode.GRID -> GridLayoutManager(ctx, UiUtils.resolveGridSpanCount(ctx))
			else -> LinearLayoutManager(ctx)
		}
		recyclerView.adapter = adapter
		recyclerView.addItemDecoration(
			when (mode) {
				ListMode.LIST -> DividerItemDecoration(ctx, RecyclerView.VERTICAL)
				ListMode.DETAILED_LIST,
				ListMode.GRID -> SpacingItemDecoration(
					resources.getDimensionPixelOffset(R.dimen.grid_spacing)
				)
			}
		)
		if (mode == ListMode.GRID) {
			recyclerView.addOnLayoutChangeListener(UiUtils.SpanCountResolver)
		}
		adapter?.notifyDataSetChanged()
		recyclerView.firstItem = position
	}

	final override fun isSection(position: Int): Boolean {
		return position == 0 || recyclerView_filter.adapter?.run {
			getItemViewType(position) != getItemViewType(position - 1)
		} ?: false
	}

	final override fun getSectionTitle(position: Int): CharSequence? {
		return when (recyclerView_filter.adapter?.getItemViewType(position)) {
			FilterAdapter.VIEW_TYPE_SORT -> getString(R.string.sort_order)
			FilterAdapter.VIEW_TYPE_TAG -> getString(R.string.genre)
			else -> null
		}
	}

	protected open fun onCreatePopupMenu(inflater: MenuInflater, menu: Menu, data: Manga) = Unit

	protected open fun onPopupMenuItemSelected(item: MenuItem, data: Manga) = false
}