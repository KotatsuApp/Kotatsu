package org.koitharu.kotatsu.reader.ui.thumbnails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseBottomSheet
import org.koitharu.kotatsu.core.ui.list.BoundsScrollListener
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.ScrollListenerInvalidationObserver
import org.koitharu.kotatsu.core.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.ui.widgets.BottomSheetHeaderBar
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetPagesBinding
import org.koitharu.kotatsu.list.ui.MangaListSpanResolver
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.thumbnails.adapter.PageThumbnailAdapter
import org.koitharu.kotatsu.reader.ui.thumbnails.adapter.TargetScrollObserver
import org.koitharu.kotatsu.util.LoggingAdapterDataObserver
import javax.inject.Inject

@AndroidEntryPoint
class PagesThumbnailsSheet :
	BaseBottomSheet<SheetPagesBinding>(),
	OnListItemClickListener<PageThumbnail>,
	BottomSheetHeaderBar.OnExpansionChangeListener {

	private val viewModel by viewModels<PagesThumbnailsViewModel>()

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private var thumbnailsAdapter: PageThumbnailAdapter? = null
	private var spanResolver: MangaListSpanResolver? = null
	private var scrollListener: ScrollListener? = null

	private val spanSizeLookup = SpanSizeLookup()
	private val listCommitCallback = Runnable {
		spanSizeLookup.invalidateCache()
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetPagesBinding {
		return SheetPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetPagesBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		spanResolver = MangaListSpanResolver(binding.root.resources)
		with(binding.headerBar) {
			title = viewModel.title
			subtitle = null
			addOnExpansionChangeListener(this@PagesThumbnailsSheet)
		}
		thumbnailsAdapter = PageThumbnailAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			clickListener = this@PagesThumbnailsSheet,
		)
		with(binding.recyclerView) {
			addItemDecoration(
				SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing)),
			)
			adapter = thumbnailsAdapter
			addOnLayoutChangeListener(spanResolver)
			spanResolver?.setGridSize(settings.gridSize / 100f, this)
			addOnScrollListener(ScrollListener().also { scrollListener = it })
			(layoutManager as GridLayoutManager).spanSizeLookup = spanSizeLookup
			thumbnailsAdapter?.registerAdapterDataObserver(
				ScrollListenerInvalidationObserver(this, checkNotNull(scrollListener)),
			)
			thumbnailsAdapter?.registerAdapterDataObserver(TargetScrollObserver(this))
			thumbnailsAdapter?.registerAdapterDataObserver(LoggingAdapterDataObserver("THUMB"))
		}
		viewModel.thumbnails.observe(viewLifecycleOwner) {
			thumbnailsAdapter?.setItems(it, listCommitCallback)
		}
		viewModel.branch.observe(viewLifecycleOwner) {
			onExpansionStateChanged(binding.headerBar, binding.headerBar.isExpanded)
		}
		viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(binding.recyclerView, this))
	}

	override fun onDestroyView() {
		spanResolver = null
		scrollListener = null
		thumbnailsAdapter = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	override fun onItemClick(item: PageThumbnail, view: View) {
		val listener = (parentFragment as? OnPageSelectListener) ?: (activity as? OnPageSelectListener)
		if (listener != null) {
			listener.onPageSelected(item.page)
		} else {
			val state = ReaderState(item.page.chapterId, item.page.index, 0)
			val intent = ReaderActivity.newIntent(view.context, viewModel.manga, state)
			startActivity(intent, scaleUpActivityOptionsOf(view).toBundle())
		}
		dismiss()
	}

	override fun onExpansionStateChanged(headerBar: BottomSheetHeaderBar, isExpanded: Boolean) {
		if (isExpanded) {
			headerBar.subtitle = viewModel.branch.value
		} else {
			headerBar.subtitle = null
		}
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
				PageThumbnailAdapter.ITEM_TYPE_THUMBNAIL -> 1
				else -> total
			}
		}

		fun invalidateCache() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}

	companion object {

		const val ARG_MANGA = "manga"
		const val ARG_CURRENT_PAGE = "current"
		const val ARG_CHAPTER_ID = "chapter_id"

		private const val TAG = "PagesThumbnailsSheet"

		fun show(fm: FragmentManager, manga: Manga, chapterId: Long, currentPage: Int = -1) {
			PagesThumbnailsSheet().withArgs(3) {
				putParcelable(ARG_MANGA, ParcelableManga(manga, true))
				putLong(ARG_CHAPTER_ID, chapterId)
				putInt(ARG_CURRENT_PAGE, currentPage)
			}.show(fm, TAG)
		}
	}
}
