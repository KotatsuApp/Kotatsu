package org.koitharu.kotatsu.details.ui.pager.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.collection.ArraySet
import androidx.core.graphics.Insets
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.list.BoundsScrollListener
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.util.PagerNestedScrollHelper
import org.koitharu.kotatsu.core.util.RecyclerViewScrollCallback
import org.koitharu.kotatsu.core.util.ext.dismissParentDialog
import org.koitharu.kotatsu.core.util.ext.findAppCompatDelegate
import org.koitharu.kotatsu.core.util.ext.findParentCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.databinding.FragmentPagesBinding
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesViewModel
import org.koitharu.kotatsu.list.ui.GridSpanResolver
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.reader.ui.PageSaveHelper
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.reader.ui.ReaderNavigationCallback
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class PagesFragment :
	BaseFragment<FragmentPagesBinding>(),
	OnListItemClickListener<PageThumbnail>, ListSelectionController.Callback {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var pageSaveHelperFactory: PageSaveHelper.Factory

	private val parentViewModel by ChaptersPagesViewModel.ActivityVMLazy(this)
	private val viewModel by viewModels<PagesViewModel>()
	private lateinit var pageSaveHelper: PageSaveHelper

	private var thumbnailsAdapter: PageThumbnailAdapter? = null
	private var spanResolver: GridSpanResolver? = null
	private var scrollListener: ScrollListener? = null
	private var selectionController: ListSelectionController? = null

	private val spanSizeLookup = SpanSizeLookup()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		pageSaveHelper = pageSaveHelperFactory.create(this)
		combine(
			parentViewModel.mangaDetails,
			parentViewModel.readingState,
			parentViewModel.selectedBranch,
		) { details, readingState, branch ->
			if (details != null && (details.isLoaded || details.chapters.isNotEmpty())) {
				PagesViewModel.State(details.filterChapters(branch), readingState, branch)
			} else {
				null
			}
		}.flowOn(Dispatchers.Default)
			.observe(this, viewModel::updateState)
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPagesBinding {
		return FragmentPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: FragmentPagesBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		spanResolver = GridSpanResolver(binding.root.resources)
		selectionController = ListSelectionController(
			appCompatDelegate = checkNotNull(findAppCompatDelegate()),
			decoration = PagesSelectionDecoration(binding.root.context),
			registryOwner = this,
			callback = this,
		)
		thumbnailsAdapter = PageThumbnailAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			clickListener = this@PagesFragment,
		)
		viewModel.gridScale.observe(viewLifecycleOwner, ::onGridScaleChanged) // before rv initialization
		with(binding.recyclerView) {
			addItemDecoration(TypedListSpacingDecoration(context, false))
			checkNotNull(selectionController).attachToRecyclerView(this)
			adapter = thumbnailsAdapter
			setHasFixedSize(true)
			PagerNestedScrollHelper(this).bind(viewLifecycleOwner)
			addOnLayoutChangeListener(spanResolver)
			addOnScrollListener(ScrollListener().also { scrollListener = it })
			(layoutManager as GridLayoutManager).let {
				it.spanSizeLookup = spanSizeLookup
				it.spanCount = checkNotNull(spanResolver).spanCount
			}
		}
		parentViewModel.isChaptersEmpty.observe(viewLifecycleOwner, ::onNoChaptersChanged)
		viewModel.thumbnails.observe(viewLifecycleOwner, ::onThumbnailsChanged)
		viewModel.onPageSaved.observeEvent(this, PagesSavedObserver(binding.recyclerView))
		viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(binding.recyclerView, this))
		viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.showOrHide(it) }
		viewModel.isLoadingUp.observe(viewLifecycleOwner) { binding.progressBarTop.showOrHide(it) }
		viewModel.isLoadingDown.observe(viewLifecycleOwner) { binding.progressBarBottom.showOrHide(it) }
	}

	override fun onDestroyView() {
		spanResolver = null
		scrollListener = null
		thumbnailsAdapter = null
		selectionController = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun onItemClick(item: PageThumbnail, view: View) {
		if (selectionController?.onItemClick(item.page.id) == true) {
			return
		}
		val listener = findParentCallback(ReaderNavigationCallback::class.java)
		if (listener != null && listener.onPageSelected(item.page)) {
			dismissParentDialog()
		} else {
			startActivity(
				IntentBuilder(view.context)
					.manga(parentViewModel.getMangaOrNull() ?: return)
					.state(ReaderState(item.page.chapterId, item.page.index, 0))
					.build(),
			)
		}
	}

	override fun onItemLongClick(item: PageThumbnail, view: View): Boolean {
		return selectionController?.onItemLongClick(view, item.page.id) ?: false
	}

	override fun onItemContextClick(item: PageThumbnail, view: View): Boolean {
		return selectionController?.onItemContextClick(view, item.page.id) ?: false
	}

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		viewBinding?.recyclerView?.invalidateItemDecorations()
	}

	override fun onCreateActionMode(
		controller: ListSelectionController,
		menuInflater: MenuInflater,
		menu: Menu,
	): Boolean {
		menuInflater.inflate(R.menu.mode_pages, menu)
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode?, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_save -> {
				viewModel.savePages(pageSaveHelper, collectSelectedPages())
				mode?.finish()
				true
			}

			else -> false
		}
	}

	private suspend fun onThumbnailsChanged(list: List<ListModel>) {
		val adapter = thumbnailsAdapter ?: return
		if (adapter.itemCount == 0) {
			var position = list.indexOfFirst { it is PageThumbnail && it.isCurrent }
			if (position > 0) {
				val spanCount = spanResolver?.spanCount ?: 0
				val offset = if (position > spanCount + 1) {
					(resources.getDimensionPixelSize(R.dimen.manga_list_details_item_height) * 0.6).roundToInt()
				} else {
					position = 0
					0
				}
				val scrollCallback = RecyclerViewScrollCallback(requireViewBinding().recyclerView, position, offset)
				adapter.emit(list)
				scrollCallback.run()
			} else {
				adapter.emit(list)
			}
		} else {
			adapter.emit(list)
		}
		spanSizeLookup.invalidateCache()
		viewBinding?.recyclerView?.let {
			scrollListener?.postInvalidate(it)
		}
	}

	private fun onGridScaleChanged(scale: Float) {
		spanSizeLookup.invalidateCache()
		spanResolver?.setGridSize(scale, requireViewBinding().recyclerView)
	}

	private fun onNoChaptersChanged(isNoChapters: Boolean) {
		with(viewBinding ?: return) {
			textViewHolder.isVisible = isNoChapters
			recyclerView.isInvisible = isNoChapters
		}
	}

	private fun collectSelectedPages(): Set<ReaderPage> {
		val checkedIds = selectionController?.peekCheckedIds() ?: return emptySet()
		val items = thumbnailsAdapter?.items ?: return emptySet()
		val result = ArraySet<ReaderPage>(checkedIds.size)
		for (item in items) {
			if (item is PageThumbnail && item.page.id in checkedIds) {
				result.add(item.page)
			}
		}
		return result
	}

	private inner class ScrollListener : BoundsScrollListener(3, 3) {

		override fun onScrolledToStart(recyclerView: RecyclerView) {
			viewModel.loadPrevChapter()
		}

		override fun onScrolledToEnd(recyclerView: RecyclerView) {
			viewModel.loadNextChapter()
		}
	}

	private inner class SpanSizeLookup : GridLayoutManager.SpanSizeLookup() {

		init {
			isSpanIndexCacheEnabled = true
			isSpanGroupIndexCacheEnabled = true
		}

		override fun getSpanSize(position: Int): Int {
			val total = (viewBinding?.recyclerView?.layoutManager as? GridLayoutManager)?.spanCount ?: return 1
			return when (thumbnailsAdapter?.getItemViewType(position)) {
				ListItemType.PAGE_THUMB.ordinal -> 1
				else -> total
			}
		}

		fun invalidateCache() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}
}
