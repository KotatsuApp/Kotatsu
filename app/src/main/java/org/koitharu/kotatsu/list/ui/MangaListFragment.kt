package org.koitharu.kotatsu.list.ui

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
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.ItemTypeDividerDecoration
import org.koitharu.kotatsu.base.ui.list.decor.SectionItemDecoration
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.list.ui.filter.FilterAdapter
import org.koitharu.kotatsu.list.ui.filter.OnFilterChangedListener
import org.koitharu.kotatsu.utils.ext.clearItemDecorations
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.hasItems
import org.koitharu.kotatsu.utils.ext.toggleDrawer

abstract class MangaListFragment : BaseFragment(R.layout.fragment_list),
	PaginationScrollListener.Callback, OnListItemClickListener<Manga>, OnFilterChangedListener,
	SectionItemDecoration.Callback, SwipeRefreshLayout.OnRefreshListener {

	private var adapter: MangaListAdapter? = null
	private var paginationListener: PaginationScrollListener? = null
	private val spanResolver = MangaListSpanResolver()
	private val spanSizeLookup = SpanSizeLookup()
	open val isSwipeRefreshEnabled = true

	protected abstract val viewModel: MangaListViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
		adapter = MangaListAdapter(get(), this)
		paginationListener = PaginationScrollListener(4, this)
		recyclerView.setHasFixedSize(true)
		recyclerView.adapter = adapter
		recyclerView.addOnScrollListener(paginationListener!!)
		swipeRefreshLayout.setOnRefreshListener(this)
		swipeRefreshLayout.isEnabled = isSwipeRefreshEnabled
		recyclerView_filter.setHasFixedSize(true)
		recyclerView_filter.addItemDecoration(ItemTypeDividerDecoration(view.context))
		recyclerView_filter.addItemDecoration(SectionItemDecoration(false, this))

		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.filter.observe(viewLifecycleOwner, ::onInitFilter)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.listMode.observe(viewLifecycleOwner, ::onListModeChanged)
		viewModel.gridScale.observe(viewLifecycleOwner, ::onGridScaleChanged)
		viewModel.isEmptyState.observe(viewLifecycleOwner, ::onEmptyStateChanged)
	}

	override fun onDestroyView() {
		adapter = null
		paginationListener = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
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

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	override fun onItemLongClick(item: Manga, view: View): Boolean {
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

	@CallSuper
	override fun onRefresh() {
		swipeRefreshLayout.isRefreshing = true
	}

	private fun onListChanged(list: List<Any>) {
		spanSizeLookup.invalidateCache()
		adapter?.items = list
	}

	private fun onError(e: Throwable) {
		if (e is CloudFlareProtectedException) {
			CloudFlareDialog.newInstance(e.url).show(childFragmentManager, CloudFlareDialog.TAG)
		}
		if (viewModel.isEmptyState.value == true) {
			textView_holder.text = e.getDisplayMessage(resources)
			textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(
				0,
				R.drawable.ic_error_large,
				0,
				0
			)
			layout_holder.isVisible = true
		} else {
			Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT)
				.show()
		}
	}

	@CallSuper
	protected open fun onLoadingStateChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems && viewModel.isEmptyState.value != true
		swipeRefreshLayout.isEnabled = isSwipeRefreshEnabled && !progressBar.isVisible
		if (!isLoading) {
			swipeRefreshLayout.isRefreshing = false
		}
	}

	private fun onEmptyStateChanged(isEmpty: Boolean) {
		if (isEmpty) {
			setUpEmptyListHolder()
		}
		layout_holder.isVisible = isEmpty
	}

	protected fun onInitFilter(config: MangaFilterConfig) {
		recyclerView_filter.adapter = FilterAdapter(
			sortOrders = config.sortOrders,
			tags = config.tags,
			state = config.currentFilter,
			listener = this
		)
		drawer?.setDrawerLockMode(
			if (config.sortOrders.isEmpty() && config.tags.isEmpty()) {
				DrawerLayout.LOCK_MODE_LOCKED_CLOSED
			} else {
				DrawerLayout.LOCK_MODE_UNLOCKED
			}
		) ?: divider_filter?.let {
			it.isGone = config.sortOrders.isEmpty() && config.tags.isEmpty()
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

	private fun onGridScaleChanged(scale: Float) {
		spanSizeLookup.invalidateCache()
		spanResolver.setGridSize(scale, recyclerView)
	}

	private fun onListModeChanged(mode: ListMode) {
		spanSizeLookup.invalidateCache()
		with(recyclerView) {
			clearItemDecorations()
			removeOnLayoutChangeListener(spanResolver)
			when (mode) {
				ListMode.LIST -> {
					layoutManager = LinearLayoutManager(context)
					addItemDecoration(
						DividerItemDecoration(
							context,
							RecyclerView.VERTICAL
						)
					)
				}
				ListMode.DETAILED_LIST -> {
					layoutManager = LinearLayoutManager(context)
					addItemDecoration(
						SpacingItemDecoration(
							resources.getDimensionPixelOffset(R.dimen.grid_spacing)
						)
					)
				}
				ListMode.GRID -> {
					layoutManager = GridLayoutManager(context, spanResolver.spanCount).also {
						it.spanSizeLookup = spanSizeLookup
					}
					addItemDecoration(
						SpacingItemDecoration(
							resources.getDimensionPixelOffset(R.dimen.grid_spacing)
						)
					)
					addOnLayoutChangeListener(spanResolver)
				}
			}
		}
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

	private inner class SpanSizeLookup : GridLayoutManager.SpanSizeLookup() {

		init {
			isSpanIndexCacheEnabled = true
			isSpanGroupIndexCacheEnabled = true
		}

		override fun getSpanSize(position: Int): Int {
			val total = (recyclerView.layoutManager as? GridLayoutManager)?.spanCount ?: return 1
			return when (adapter?.getItemViewType(position)) {
				MangaListAdapter.ITEM_TYPE_PROGRESS -> total
				else -> 1
			}
		}

		fun invalidateCache() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}
}