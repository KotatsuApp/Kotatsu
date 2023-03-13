package org.koitharu.kotatsu.reader.ui.thumbnails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.base.ui.widgets.BottomSheetHeaderBar
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPages
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.SheetPagesBinding
import org.koitharu.kotatsu.list.ui.MangaListSpanResolver
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.thumbnails.adapter.PageThumbnailAdapter
import org.koitharu.kotatsu.utils.ext.getParcelableCompat
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import org.koitharu.kotatsu.utils.ext.withArgs
import javax.inject.Inject

@AndroidEntryPoint
class PagesThumbnailsSheet :
	BaseBottomSheet<SheetPagesBinding>(),
	OnListItemClickListener<MangaPage>,
	BottomSheetHeaderBar.OnExpansionChangeListener {

	@Inject
	lateinit var mangaRepositoryFactory: MangaRepository.Factory

	@Inject
	lateinit var pageLoader: PageLoader

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private lateinit var thumbnails: List<PageThumbnail>
	private var spanResolver: MangaListSpanResolver? = null
	private var currentPageIndex = -1

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val pages = arguments?.getParcelableCompat<ParcelableMangaPages>(ARG_PAGES)?.pages
		if (pages.isNullOrEmpty()) {
			dismissAllowingStateLoss()
			return
		}
		currentPageIndex = requireArguments().getInt(ARG_CURRENT, currentPageIndex)
		val repository = mangaRepositoryFactory.create(pages.first().source)
		thumbnails = pages.mapIndexed { i, x ->
			PageThumbnail(
				number = i + 1,
				isCurrent = i == currentPageIndex,
				repository = repository,
				page = x,
			)
		}
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetPagesBinding {
		return SheetPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		spanResolver = MangaListSpanResolver(view.resources)
		with(binding.headerBar) {
			title = arguments?.getString(ARG_TITLE)
			subtitle = null
			addOnExpansionChangeListener(this@PagesThumbnailsSheet)
		}

		with(binding.recyclerView) {
			addItemDecoration(
				SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing)),
			)
			adapter = PageThumbnailAdapter(
				dataSet = thumbnails,
				coil = coil,
				scope = viewLifecycleScope,
				loader = pageLoader,
				clickListener = this@PagesThumbnailsSheet,
			)
			addOnLayoutChangeListener(spanResolver)
			spanResolver?.setGridSize(settings.gridSize / 100f, this)
			if (currentPageIndex > 0) {
				val offset = resources.getDimensionPixelOffset(R.dimen.preferred_grid_width)
				(layoutManager as GridLayoutManager).scrollToPositionWithOffset(currentPageIndex, offset)
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		spanResolver = null
	}

	override fun onItemClick(item: MangaPage, view: View) {
		(
			(parentFragment as? OnPageSelectListener)
				?: (activity as? OnPageSelectListener)
			)?.run {
				onPageSelected(item)
				dismiss()
			}
	}

	override fun onExpansionStateChanged(headerBar: BottomSheetHeaderBar, isExpanded: Boolean) {
		if (isExpanded) {
			headerBar.subtitle = resources.getQuantityString(
				R.plurals.pages,
				thumbnails.size,
				thumbnails.size,
			)
		} else {
			headerBar.subtitle = null
		}
	}

	companion object {

		private const val ARG_PAGES = "pages"
		private const val ARG_TITLE = "title"
		private const val ARG_CURRENT = "current"

		private const val TAG = "PagesThumbnailsSheet"

		fun show(fm: FragmentManager, pages: List<MangaPage>, title: String, currentPage: Int) =
			PagesThumbnailsSheet().withArgs(3) {
				putParcelable(ARG_PAGES, ParcelableMangaPages(pages))
				putString(ARG_TITLE, title)
				putInt(ARG_CURRENT, currentPage)
			}.show(fm, TAG)
	}
}
