package org.koitharu.kotatsu.reader.ui.thumbnails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPages
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.SheetPagesBinding
import org.koitharu.kotatsu.list.ui.MangaListSpanResolver
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderViewModel
import org.koitharu.kotatsu.reader.ui.thumbnails.adapter.PageThumbnailAdapter
import org.koitharu.kotatsu.utils.BottomSheetToolbarController
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import org.koitharu.kotatsu.utils.ext.withArgs

class PagesThumbnailsSheet :
	BaseBottomSheet<SheetPagesBinding>(),
	OnListItemClickListener<MangaPage> {

	private lateinit var thumbnails: List<PageThumbnail>
	private val spanResolver = MangaListSpanResolver()
	private var currentPageIndex = -1
	private var pageLoader: PageLoader? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val pages = arguments?.getParcelable<ParcelableMangaPages>(ARG_PAGES)?.pages
		if (pages.isNullOrEmpty()) {
			dismissAllowingStateLoss()
			return
		}
		currentPageIndex = requireArguments().getInt(ARG_CURRENT, currentPageIndex)
		val repository = MangaRepository(pages.first().source)
		thumbnails = pages.mapIndexed { i, x ->
			PageThumbnail(
				number = i + 1,
				isCurrent = i == currentPageIndex,
				repository = repository,
				page = x
			)
		}
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetPagesBinding {
		return SheetPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val title = arguments?.getString(ARG_TITLE)
		binding.toolbar.title = title
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		binding.toolbar.subtitle = null
		behavior?.addBottomSheetCallback(ToolbarController(binding.toolbar))

		if (!resources.getBoolean(R.bool.is_tablet)) {
			binding.toolbar.navigationIcon = null
		} else {
			binding.toolbar.subtitle =
				resources.getQuantityString(R.plurals.pages, thumbnails.size, thumbnails.size)
		}

		with(binding.recyclerView) {
			addItemDecoration(
				SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing))
			)
			adapter = PageThumbnailAdapter(
				dataSet = thumbnails,
				coil = get(),
				scope = viewLifecycleScope,
				loader = getPageLoader(),
				clickListener = this@PagesThumbnailsSheet
			)
			addOnLayoutChangeListener(spanResolver)
			spanResolver.setGridSize(get<AppSettings>().gridSize / 100f, this)
			if (currentPageIndex > 0) {
				val offset = resources.getDimensionPixelOffset(R.dimen.preferred_grid_width)
				(layoutManager as GridLayoutManager).scrollToPositionWithOffset(currentPageIndex, offset)
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		pageLoader?.close()
		pageLoader = null
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

	private fun getPageLoader(): PageLoader {
		val viewModel = (activity as? ReaderActivity)?.getViewModel<ReaderViewModel>()
		return viewModel?.pageLoader ?: PageLoader().also { pageLoader = it }
	}

	private inner class ToolbarController(toolbar: Toolbar) : BottomSheetToolbarController(toolbar) {
		override fun onStateChanged(bottomSheet: View, newState: Int) {
			super.onStateChanged(bottomSheet, newState)
			if (newState == BottomSheetBehavior.STATE_EXPANDED) {
				toolbar.subtitle = resources.getQuantityString(
					R.plurals.pages,
					thumbnails.size,
					thumbnails.size
				)
			} else {
				toolbar.subtitle = null
			}
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