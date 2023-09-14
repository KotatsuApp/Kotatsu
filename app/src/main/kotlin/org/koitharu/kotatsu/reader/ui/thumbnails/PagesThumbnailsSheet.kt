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
import org.koitharu.kotatsu.core.ui.list.BoundsScrollListener
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetBehavior
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetCallback
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.RecyclerViewScrollCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.plus
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetPagesBinding
import org.koitharu.kotatsu.list.ui.MangaListSpanResolver
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.thumbnails.adapter.PageThumbnailAdapter
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class PagesThumbnailsSheet :
	BaseAdaptiveSheet<SheetPagesBinding>(),
	AdaptiveSheetCallback,
	OnListItemClickListener<PageThumbnail> {

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
		addSheetCallback(this)
		spanResolver = MangaListSpanResolver(binding.root.resources)
		thumbnailsAdapter = PageThumbnailAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			clickListener = this@PagesThumbnailsSheet,
		)
		with(binding.recyclerView) {
			addItemDecoration(TypedListSpacingDecoration(context, false))
			adapter = thumbnailsAdapter
			addOnLayoutChangeListener(spanResolver)
			spanResolver?.setGridSize(settings.gridSize / 100f, this)
			addOnScrollListener(ScrollListener().also { scrollListener = it })
			(layoutManager as GridLayoutManager).spanSizeLookup = spanSizeLookup
		}
		viewModel.thumbnails.observe(viewLifecycleOwner, ::onThumbnailsChanged)
		viewModel.branch.observe(viewLifecycleOwner, ::updateTitle)
		viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(binding.recyclerView, this))
		viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.showOrHide(it) }
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
			val intent = IntentBuilder(view.context).manga(viewModel.manga).state(state).build()
			startActivity(intent)
		}
		dismiss()
	}

	override fun onStateChanged(sheet: View, newState: Int) {
		viewBinding?.recyclerView?.isFastScrollerEnabled = newState == AdaptiveSheetBehavior.STATE_EXPANDED
	}

	private fun updateTitle(branch: String?) {
		val mangaName = viewModel.manga.title
		viewBinding?.headerBar?.title = if (branch != null) {
			getString(R.string.manga_branch_title_template, mangaName, branch)
		} else {
			mangaName
		}
	}

	private fun onThumbnailsChanged(list: List<ListModel>) {
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
				adapter.setItems(list, listCommitCallback + scrollCallback)
			} else {
				adapter.setItems(list, listCommitCallback)
			}
		} else {
			adapter.setItems(list, listCommitCallback)
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
				ListItemType.PAGE_THUMB.ordinal -> 1
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
				putParcelable(ARG_MANGA, ParcelableManga(manga))
				putLong(ARG_CHAPTER_ID, chapterId)
				putInt(ARG_CURRENT_PAGE, currentPage)
			}.showDistinct(fm, TAG)
		}
	}
}
