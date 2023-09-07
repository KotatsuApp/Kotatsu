package org.koitharu.kotatsu.bookmarks.ui.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetBehavior
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetCallback
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.RecyclerViewScrollCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.plus
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetPagesBinding
import org.koitharu.kotatsu.list.ui.MangaListSpanResolver
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.thumbnails.OnPageSelectListener
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class BookmarksSheet :
	BaseAdaptiveSheet<SheetPagesBinding>(),
	AdaptiveSheetCallback,
	OnListItemClickListener<Bookmark> {

	private val viewModel by viewModels<BookmarksSheetViewModel>()

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private var bookmarksAdapter: BookmarksAdapter? = null
	private var spanResolver: MangaListSpanResolver? = null

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
		bookmarksAdapter = BookmarksAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			clickListener = this@BookmarksSheet,
			headerClickListener = null,
		)
		viewBinding?.headerBar?.setTitle(R.string.bookmarks)
		with(binding.recyclerView) {
			addItemDecoration(TypedListSpacingDecoration(context, false))
			adapter = bookmarksAdapter
			addOnLayoutChangeListener(spanResolver)
			spanResolver?.setGridSize(settings.gridSize / 100f, this)
			(layoutManager as GridLayoutManager).spanSizeLookup = spanSizeLookup
		}
		viewModel.content.observe(viewLifecycleOwner, ::onThumbnailsChanged)
		viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(binding.recyclerView, this))
	}

	override fun onDestroyView() {
		spanResolver = null
		bookmarksAdapter = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	override fun onItemClick(item: Bookmark, view: View) {
		val listener = (parentFragment as? OnPageSelectListener) ?: (activity as? OnPageSelectListener)
		if (listener != null) {
			listener.onPageSelected(ReaderPage(item.toMangaPage(), item.page, item.chapterId))
		} else {
			val intent = IntentBuilder(view.context)
				.manga(viewModel.manga)
				.bookmark(item)
				.incognito(true)
				.build()
			startActivity(intent)
		}
		dismiss()
	}

	override fun onStateChanged(sheet: View, newState: Int) {
		viewBinding?.recyclerView?.isFastScrollerEnabled = newState == AdaptiveSheetBehavior.STATE_EXPANDED
	}

	private fun onThumbnailsChanged(list: List<ListModel>) {
		val adapter = bookmarksAdapter ?: return
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

	private inner class SpanSizeLookup : GridLayoutManager.SpanSizeLookup() {

		init {
			isSpanIndexCacheEnabled = true
			isSpanGroupIndexCacheEnabled = true
		}

		override fun getSpanSize(position: Int): Int {
			val total = (viewBinding?.recyclerView?.layoutManager as? GridLayoutManager)?.spanCount ?: return 1
			return when (bookmarksAdapter?.getItemViewType(position)) {
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

		private const val TAG = "BookmarksSheet"

		fun show(fm: FragmentManager, manga: Manga) {
			BookmarksSheet().withArgs(1) {
				putParcelable(ARG_MANGA, ParcelableManga(manga))
			}.showDistinct(fm, TAG)
		}
	}
}
