package org.koitharu.kotatsu.list.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_list.*
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.ProgressBarAdapter
import org.koitharu.kotatsu.base.ui.list.decor.ItemTypeDividerDecoration
import org.koitharu.kotatsu.base.ui.list.decor.SectionItemDecoration
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.filter.FilterAdapter
import org.koitharu.kotatsu.list.ui.filter.OnFilterChangedListener
import org.koitharu.kotatsu.utils.UiUtils
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListFragment : BaseFragment(R.layout.fragment_list),
	PaginationScrollListener.Callback, OnRecyclerItemClickListener<Manga>,
	SharedPreferences.OnSharedPreferenceChangeListener, OnFilterChangedListener,
	SectionItemDecoration.Callback, SwipeRefreshLayout.OnRefreshListener {

	private val settings by inject<AppSettings>()
	private val adapterConfig = ConcatAdapter.Config.Builder()
		.setIsolateViewTypes(true)
		.setStableIdMode(ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS)
		.build()

	private var adapter: MangaListAdapter? = null
	private var progressAdapter: ProgressBarAdapter? = null
	private var paginationListener: PaginationScrollListener? = null
	protected var isSwipeRefreshEnabled = true

	protected abstract val viewModel: MangaListViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
		adapter = MangaListAdapter(this)
		progressAdapter = ProgressBarAdapter()
		paginationListener = PaginationScrollListener(4, this)
		recyclerView.setHasFixedSize(true)
		initListMode(settings.listMode)
		recyclerView.addOnScrollListener(paginationListener!!)
		swipeRefreshLayout.setOnRefreshListener(this)
		recyclerView_filter.setHasFixedSize(true)
		recyclerView_filter.addItemDecoration(ItemTypeDividerDecoration(view.context))
		recyclerView_filter.addItemDecoration(SectionItemDecoration(false, this))
		settings.subscribe(this)
		if (savedInstanceState == null) {
			onRequestMoreItems(0)
		}
		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.filter.observe(viewLifecycleOwner, ::onInitFilter)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
	}

	override fun onDestroyView() {
		adapter = null
		progressAdapter = null
		paginationListener = null
		settings.unsubscribe(this)
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

	override fun onItemClick(item: Manga, position: Int, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
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
		swipeRefreshLayout.isRefreshing = true
		onRequestMoreItems(0)
	}

	private fun onListChanged(list: List<Manga>) {
		paginationListener?.reset()
		adapter?.replaceData(list)
		if (list.isEmpty()) {
			setUpEmptyListHolder()
			layout_holder.isVisible = true
		} else {
			layout_holder.isVisible = false
		}
		progressAdapter?.isProgressVisible = list.isNotEmpty()
		recyclerView.callOnScrollListeners()
	}

	private fun onError(e: Throwable) {
		if (e is CloudFlareProtectedException) {
			CloudFlareDialog.newInstance(e.url).show(childFragmentManager, CloudFlareDialog.TAG)
		}
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

	@CallSuper
	protected open fun onLoadingStateChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems
		swipeRefreshLayout.isEnabled = isSwipeRefreshEnabled && !progressBar.isVisible
		if (isLoading) {
			layout_holder.isVisible = false
		} else {
			swipeRefreshLayout.isRefreshing = false
		}
	}

	@CallSuper
	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		if (context == null) {
			return
		}
		when (key) {
			AppSettings.KEY_LIST_MODE -> initListMode(settings.listMode)
			AppSettings.KEY_GRID_SIZE -> UiUtils.SpanCountResolver.update(recyclerView)
		}
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

	private fun initListMode(mode: ListMode) {
		val ctx = context ?: return
		val position = recyclerView.firstItem
		recyclerView.adapter = null
		recyclerView.layoutManager = null
		recyclerView.clearItemDecorations()
		recyclerView.removeOnLayoutChangeListener(UiUtils.SpanCountResolver)
		adapter?.listMode = mode
		recyclerView.layoutManager = when (mode) {
			ListMode.GRID -> {
				GridLayoutManager(ctx, UiUtils.resolveGridSpanCount(ctx)).apply {
					spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
						override fun getSpanSize(position: Int) = if (position < getItemsCount())
							1 else this@apply.spanCount
					}
				}
			}
			else -> LinearLayoutManager(ctx)
		}
		recyclerView.recycledViewPool.clear()
		recyclerView.adapter = ConcatAdapter(adapterConfig, adapter, progressAdapter)
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

	override fun getItemsCount() = adapter?.itemCount ?: 0

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