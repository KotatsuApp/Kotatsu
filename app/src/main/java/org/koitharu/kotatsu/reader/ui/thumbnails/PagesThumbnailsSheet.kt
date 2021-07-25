package org.koitharu.kotatsu.reader.ui.thumbnails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.SheetPagesBinding
import org.koitharu.kotatsu.list.ui.MangaListSpanResolver
import org.koitharu.kotatsu.reader.ui.thumbnails.adapter.PageThumbnailAdapter
import org.koitharu.kotatsu.utils.ext.mangaRepositoryOf
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import org.koitharu.kotatsu.utils.ext.withArgs


class PagesThumbnailsSheet : BaseBottomSheet<SheetPagesBinding>(),
	OnListItemClickListener<MangaPage> {

	private lateinit var thumbnails: List<PageThumbnail>
	private val spanResolver = MangaListSpanResolver()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val pages = arguments?.getParcelableArrayList<MangaPage>(ARG_PAGES)
		if (pages.isNullOrEmpty()) {
			dismissAllowingStateLoss()
			return
		}
		val current = arguments?.getInt(ARG_CURRENT, -1) ?: -1
		val repository = mangaRepositoryOf(pages.first().source)
		thumbnails = pages.mapIndexed { i, x ->
			PageThumbnail(
				number = i + 1,
				isCurrent = i == current,
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
		binding.toolbar.elevation = 0F
		binding.toolbar.title = title
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		binding.toolbar.subtitle =
			resources.getQuantityString(R.plurals.pages, thumbnails.size, thumbnails.size)

		val initialTopPosition = binding.recyclerView.top

		with(binding.recyclerView) {
			addItemDecoration(
				SpacingItemDecoration(view.resources.getDimensionPixelOffset(R.dimen.grid_spacing))
			)
			adapter = PageThumbnailAdapter(
				thumbnails,
				get(),
				viewLifecycleScope,
				get(),
				this@PagesThumbnailsSheet
			)
			addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) = Unit

				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					super.onScrolled(recyclerView, dx, dy)
					binding.appbar.isLifted = getChildAt(0).top < initialTopPosition
				}
			})
			addOnLayoutChangeListener(spanResolver)
			spanResolver.setGridSize(get<AppSettings>().gridSize / 100f, this)
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?) =
		super.onCreateDialog(savedInstanceState).also {
			val behavior = (it as? BottomSheetDialog)?.behavior ?: return@also
			behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

				override fun onStateChanged(bottomSheet: View, newState: Int) = Unit
			})

		}

	override fun onItemClick(item: MangaPage, view: View) {
		((parentFragment as? OnPageSelectListener)
			?: (activity as? OnPageSelectListener))?.run {
			onPageSelected(item)
			dismiss()
		}
	}

	companion object {

		private const val ARG_PAGES = "pages"
		private const val ARG_TITLE = "title"
		private const val ARG_CURRENT = "current"

		private const val TAG = "PagesThumbnailsSheet"

		fun show(fm: FragmentManager, pages: List<MangaPage>, title: String, currentPage: Int) =
			PagesThumbnailsSheet().withArgs(3) {
				putParcelableArrayList(ARG_PAGES, ArrayList<MangaPage>(pages))
				putString(ARG_TITLE, title)
				putInt(ARG_CURRENT, currentPage)
			}.show(fm, TAG)

	}
}