package org.koitharu.kotatsu.list.ui

import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.GravityCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.FitHeightGridLayoutManager
import org.koitharu.kotatsu.base.ui.list.FitHeightLinearLayoutManager
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter.Companion.ITEM_TYPE_MANGA_GRID
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.main.ui.MainActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListFragment :
	BaseFragment<FragmentListBinding>(),
	PaginationScrollListener.Callback,
	MangaListListener,
	SwipeRefreshLayout.OnRefreshListener {

	private var listAdapter: MangaListAdapter? = null
	private var paginationListener: PaginationScrollListener? = null
	private val spanResolver = MangaListSpanResolver()
	private val spanSizeLookup = SpanSizeLookup()
	private val listCommitCallback = Runnable {
		spanSizeLookup.invalidateCache()
	}
	open val isSwipeRefreshEnabled = true

	protected abstract val viewModel: MangaListViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentListBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		listAdapter = MangaListAdapter(
			coil = get(),
			lifecycleOwner = viewLifecycleOwner,
			listener = this,
		)
		paginationListener = PaginationScrollListener(4, this)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = listAdapter
			addOnScrollListener(paginationListener!!)
		}
		with(binding.swipeRefreshLayout) {
			setProgressBackgroundColorSchemeColor(context.getThemeColor(com.google.android.material.R.attr.colorPrimary))
			setColorSchemeColors(context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
			setOnRefreshListener(this@MangaListFragment)
			isEnabled = isSwipeRefreshEnabled
		}

		viewModel.listMode.observe(viewLifecycleOwner, ::onListModeChanged)
		viewModel.gridScale.observe(viewLifecycleOwner, ::onGridScaleChanged)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		listAdapter = null
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
		else -> super.onOptionsItemSelected(item)
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
		binding.swipeRefreshLayout.isRefreshing = true
		viewModel.onRefresh()
	}

	private fun onListChanged(list: List<ListModel>) {
		listAdapter?.setItems(list, listCommitCallback)
	}

	private fun onError(e: Throwable) {
		if (e is CloudFlareProtectedException) {
			CloudFlareDialog.newInstance(e.url).show(childFragmentManager, CloudFlareDialog.TAG)
		} else {
			Snackbar.make(
				binding.recyclerView,
				e.getDisplayMessage(resources),
				Snackbar.LENGTH_SHORT
			).show()
		}
	}

	private fun resolveException(e: Throwable) {
		if (ExceptionResolver.canResolve(e)) {
			viewLifecycleScope.launch {
				if (exceptionResolver.resolve(e)) {
					viewModel.onRetry()
				}
			}
		} else {
			viewModel.onRetry()
		}
	}

	@CallSuper
	protected open fun onLoadingStateChanged(isLoading: Boolean) {
		binding.swipeRefreshLayout.isEnabled = binding.swipeRefreshLayout.isRefreshing ||
			isSwipeRefreshEnabled && !isLoading
		if (!isLoading) {
			binding.swipeRefreshLayout.isRefreshing = false
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val headerHeight = (activity as? AppBarOwner)?.appBar?.measureHeight() ?: insets.top
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		if (activity is MainActivity) {
			binding.recyclerView.updatePadding(
				top = headerHeight,
				bottom = insets.bottom,
			)
			binding.swipeRefreshLayout.setProgressViewOffset(
				true,
				headerHeight + resources.resolveDp(-72),
				headerHeight + resources.resolveDp(10),
			)
		}
	}

	override fun onFilterClick() = Unit

	override fun onEmptyActionClick() = Unit

	override fun onRetryClick(error: Throwable) {
		resolveException(error)
	}

	override fun onTagRemoveClick(tag: MangaTag) {
		viewModel.onRemoveFilterTag(tag)
	}

	private fun onGridScaleChanged(scale: Float) {
		spanSizeLookup.invalidateCache()
		spanResolver.setGridSize(scale, binding.recyclerView)
	}

	private fun onListModeChanged(mode: ListMode) {
		spanSizeLookup.invalidateCache()
		with(binding.recyclerView) {
			clearItemDecorations()
			removeOnLayoutChangeListener(spanResolver)
			when (mode) {
				ListMode.LIST -> {
					layoutManager = FitHeightLinearLayoutManager(context)
					val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
					addItemDecoration(SpacingItemDecoration(spacing))
					updatePadding(left = spacing, right = spacing)
				}
				ListMode.DETAILED_LIST -> {
					layoutManager = FitHeightLinearLayoutManager(context)
					val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
					updatePadding(left = spacing, right = spacing)
					addItemDecoration(SpacingItemDecoration(spacing))
				}
				ListMode.GRID -> {
					layoutManager = FitHeightGridLayoutManager(context, spanResolver.spanCount).also {
						it.spanSizeLookup = spanSizeLookup
					}
					val spacing = resources.getDimensionPixelOffset(R.dimen.grid_spacing)
					addItemDecoration(SpacingItemDecoration(spacing))
					updatePadding(left = spacing, right = spacing)
					addOnLayoutChangeListener(spanResolver)
				}
			}
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
			val total =
				(binding.recyclerView.layoutManager as? GridLayoutManager)?.spanCount ?: return 1
			return when (listAdapter?.getItemViewType(position)) {
				ITEM_TYPE_MANGA_GRID -> 1
				else -> total
			}
		}

		fun invalidateCache() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}
}