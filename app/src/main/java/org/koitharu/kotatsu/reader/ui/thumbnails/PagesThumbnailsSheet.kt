package org.koitharu.kotatsu.reader.ui.thumbnails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
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
import org.koitharu.kotatsu.utils.ext.resolveDp
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
		val repository = pages.first().source.repository
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
		with(binding.recyclerView) {
			addItemDecoration(SpacingItemDecoration(view.resources.resolveDp(8)))
			adapter = PageThumbnailAdapter(
				thumbnails,
				get(),
				viewLifecycleScope,
				get(),
				this@PagesThumbnailsSheet
			)
			addOnLayoutChangeListener(spanResolver)
			spanResolver.setGridSize(get<AppSettings>().gridSize / 100f, this)
		}

		val title = arguments?.getString(ARG_TITLE)
		binding.toolbar.title = title
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		binding.toolbar.subtitle =
			resources.getQuantityString(R.plurals.pages, thumbnails.size, thumbnails.size)
		binding.textViewTitle.text = title
		if (dialog !is BottomSheetDialog) {
			binding.toolbar.isVisible = true
			binding.textViewTitle.isVisible = false
			binding.appbar.elevation = resources.getDimension(R.dimen.elevation_large)
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?) =
		super.onCreateDialog(savedInstanceState).also {
			val behavior = (it as? BottomSheetDialog)?.behavior ?: return@also
			behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
				private val elevation = resources.getDimension(R.dimen.elevation_large)

				override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_EXPANDED) {
						binding.toolbar.isVisible = true
						binding.textViewTitle.isVisible = false
						binding.appbar.elevation = elevation
					} else {
						binding.toolbar.isVisible = false
						binding.textViewTitle.isVisible = true
						binding.appbar.elevation = 0f
					}
				}
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