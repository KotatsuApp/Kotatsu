package org.koitharu.kotatsu.reader.ui.thumbnails

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.sheet_pages.*
import kotlinx.coroutines.DisposableHandle
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.reader.ui.thumbnails.adapter.PageThumbnailAdapter
import org.koitharu.kotatsu.utils.UiUtils
import org.koitharu.kotatsu.utils.ext.resolveDp
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import org.koitharu.kotatsu.utils.ext.withArgs

class PagesThumbnailsSheet : BaseBottomSheet(R.layout.sheet_pages),
	OnListItemClickListener<MangaPage> {

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		recyclerView.addItemDecoration(SpacingItemDecoration(view.resources.resolveDp(8)))
		val pages = arguments?.getParcelableArrayList<MangaPage>(ARG_PAGES)
		if (pages == null) {
			dismissAllowingStateLoss()
			return
		}
		recyclerView.adapter = PageThumbnailAdapter(get(), viewLifecycleScope, get(), this).apply {
			items = pages
		}
		val title = arguments?.getString(ARG_TITLE)
		toolbar.title = title
		toolbar.setNavigationOnClickListener { dismiss() }
		toolbar.subtitle = resources.getQuantityString(R.plurals.pages, pages.size, pages.size)
		textView_title.text = title
		if (dialog !is BottomSheetDialog) {
			toolbar.isVisible = true
			textView_title.isVisible = false
			appbar.elevation = resources.getDimension(R.dimen.elevation_large)
		}
		recyclerView.addOnLayoutChangeListener(UiUtils.SpanCountResolver)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?) =
		super.onCreateDialog(savedInstanceState).also {
			val behavior = (it as? BottomSheetDialog)?.behavior ?: return@also
			behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
				private val elevation = resources.getDimension(R.dimen.elevation_large)

				override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_EXPANDED) {
						toolbar.isVisible = true
						textView_title.isVisible = false
						appbar.elevation = elevation
					} else {
						toolbar.isVisible = false
						textView_title.isVisible = true
						appbar.elevation = 0f
					}
				}
			})

		}

	override fun onDestroyView() {
		(recyclerView.adapter as? DisposableHandle)?.dispose()
		recyclerView.adapter = null
		super.onDestroyView()
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

		private const val TAG = "PagesThumbnailsSheet"

		fun show(fm: FragmentManager, pages: List<MangaPage>, title: String) =
			PagesThumbnailsSheet().withArgs(2) {
				putParcelableArrayList(ARG_PAGES, ArrayList<MangaPage>(pages))
				putString(ARG_TITLE, title)
			}.show(fm, TAG)

	}
}