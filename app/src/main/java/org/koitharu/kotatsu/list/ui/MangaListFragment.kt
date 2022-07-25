package org.koitharu.kotatsu.list.ui

import android.os.Bundle
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.CallSuper
import androidx.appcompat.view.ActionMode
import androidx.collection.ArraySet
import androidx.core.graphics.Insets
import androidx.core.view.isNotEmpty
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.FitHeightGridLayoutManager
import org.koitharu.kotatsu.base.ui.list.FitHeightLinearLayoutManager
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.base.ui.list.decor.TypedSpacingItemDecoration
import org.koitharu.kotatsu.base.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter.Companion.ITEM_TYPE_MANGA_GRID
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.main.ui.BottomNavOwner
import org.koitharu.kotatsu.main.ui.MainActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListFragment :
	BaseFragment<FragmentListBinding>(),
	PaginationScrollListener.Callback,
	MangaListListener,
	SwipeRefreshLayout.OnRefreshListener,
	ListSelectionController.Callback2,
	FastScroller.FastScrollListener {

	@Inject
	lateinit var coil: ImageLoader

	private var listAdapter: MangaListAdapter? = null
	private var paginationListener: PaginationScrollListener? = null
	private var selectionController: ListSelectionController? = null
	private var spanResolver: MangaListSpanResolver? = null
	private val spanSizeLookup = SpanSizeLookup()
	private val listCommitCallback = Runnable {
		spanSizeLookup.invalidateCache()
	}
	open val isSwipeRefreshEnabled = true

	protected abstract val viewModel: MangaListViewModel

	protected val selectedItemsIds: Set<Long>
		get() = selectionController?.snapshot().orEmpty()

	protected val selectedItems: Set<Manga>
		get() = collectSelectedItems()

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentListBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		listAdapter = onCreateAdapter()
		spanResolver = MangaListSpanResolver(view.resources)
		selectionController = ListSelectionController(
			activity = requireActivity(),
			decoration = MangaSelectionDecoration(view.context),
			registryOwner = this,
			callback = this,
		)
		paginationListener = PaginationScrollListener(4, this)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = listAdapter
			checkNotNull(selectionController).attachToRecyclerView(binding.recyclerView)
			addOnScrollListener(paginationListener!!)
			fastScroller.setFastScrollListener(this@MangaListFragment)
		}
		with(binding.swipeRefreshLayout) {
			setProgressBackgroundColorSchemeColor(context.getThemeColor(com.google.android.material.R.attr.colorPrimary))
			setColorSchemeColors(context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
			setOnRefreshListener(this@MangaListFragment)
			isEnabled = isSwipeRefreshEnabled
		}
		addMenuProvider(MangaListMenuProvider(childFragmentManager))

		viewModel.listMode.observe(viewLifecycleOwner, ::onListModeChanged)
		viewModel.gridScale.observe(viewLifecycleOwner, ::onGridScaleChanged)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.onActionDone.observe(viewLifecycleOwner, ::onActionDone)
	}

	override fun onDestroyView() {
		listAdapter = null
		paginationListener = null
		selectionController = null
		spanResolver = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	override fun onItemClick(item: Manga, view: View) {
		if (selectionController?.onItemClick(item.id) != true) {
			startActivity(DetailsActivity.newIntent(context ?: return, item))
		}
	}

	override fun onItemLongClick(item: Manga, view: View): Boolean {
		return selectionController?.onItemLongClick(item.id) ?: false
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
				Snackbar.LENGTH_SHORT,
			).show()
		}
	}

	private fun onActionDone(action: ReversibleAction) {
		val handle = action.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make(binding.recyclerView, action.stringResId, length)
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		snackbar.show()
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

	protected open fun onCreateAdapter(): MangaListAdapter {
		return MangaListAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			listener = this,
		)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
		binding.recyclerView.fastScroller.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = insets.bottom
			marginEnd = insets.end(binding.recyclerView)
		}
		if (activity is MainActivity) {
			val headerHeight = (activity as? AppBarOwner)?.appBar?.measureHeight() ?: insets.top
			binding.swipeRefreshLayout.setProgressViewOffset(
				true,
				headerHeight + resources.resolveDp(-72),
				headerHeight + resources.resolveDp(10),
			)
		}
	}

	override fun onFilterClick(view: View?) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onListHeaderClick(item: ListHeader, view: View) = Unit

	override fun onRetryClick(error: Throwable) {
		resolveException(error)
	}

	override fun onUpdateFilter(tags: Set<MangaTag>) {
		viewModel.onUpdateFilter(tags)
	}

	private fun onGridScaleChanged(scale: Float) {
		spanSizeLookup.invalidateCache()
		spanResolver?.setGridSize(scale, binding.recyclerView)
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
					val decoration = TypedSpacingItemDecoration(
						MangaListAdapter.ITEM_TYPE_MANGA_LIST to 0,
						fallbackSpacing = spacing,
					)
					addItemDecoration(decoration)
				}
				ListMode.DETAILED_LIST -> {
					layoutManager = FitHeightLinearLayoutManager(context)
					val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
					updatePadding(left = spacing, right = spacing)
					addItemDecoration(SpacingItemDecoration(spacing))
				}
				ListMode.GRID -> {
					layoutManager = FitHeightGridLayoutManager(context, checkNotNull(spanResolver).spanCount).also {
						it.spanSizeLookup = spanSizeLookup
					}
					val spacing = resources.getDimensionPixelOffset(R.dimen.grid_spacing)
					addItemDecoration(SpacingItemDecoration(spacing))
					updatePadding(left = spacing, right = spacing)
					addOnLayoutChangeListener(spanResolver)
				}
			}
			selectionController?.attachToRecyclerView(binding.recyclerView)
		}
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		return menu.isNotEmpty()
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_select_all -> {
				val ids = listAdapter?.items?.mapNotNull {
					(it as? MangaItemModel)?.id
				} ?: return false
				selectionController?.addAll(ids)
				true
			}
			R.id.action_share -> {
				ShareHelper(requireContext()).shareMangaLinks(selectedItems)
				mode.finish()
				true
			}
			R.id.action_favourite -> {
				FavouriteCategoriesBottomSheet.show(childFragmentManager, selectedItems)
				mode.finish()
				true
			}
			R.id.action_save -> {
				DownloadService.confirmAndStart(requireContext(), selectedItems)
				mode.finish()
				true
			}
			else -> false
		}
	}

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		binding.recyclerView.invalidateItemDecorations()
	}

	override fun onFastScrollStart(fastScroller: FastScroller) {
		(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
		binding.swipeRefreshLayout.isEnabled = false
	}

	override fun onFastScrollStop(fastScroller: FastScroller) {
		binding.swipeRefreshLayout.isEnabled = isSwipeRefreshEnabled
	}

	private fun collectSelectedItems(): Set<Manga> {
		val checkedIds = selectionController?.peekCheckedIds() ?: return emptySet()
		val items = listAdapter?.items ?: return emptySet()
		val result = ArraySet<Manga>(checkedIds.size)
		for (item in items) {
			if (item is MangaItemModel && item.id in checkedIds) {
				result.add(item.manga)
			}
		}
		return result
	}

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
