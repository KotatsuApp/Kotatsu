package org.koitharu.kotatsu.details.ui.pager.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.sheet.BookmarksAdapter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.dismissParentDialog
import org.koitharu.kotatsu.core.util.ext.findParentCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.FragmentMangaBookmarksBinding
import org.koitharu.kotatsu.details.ui.DetailsViewModel
import org.koitharu.kotatsu.list.ui.GridSpanResolver
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.reader.ui.ReaderNavigationCallback
import javax.inject.Inject

@AndroidEntryPoint
class MangaBookmarksFragment : BaseFragment<FragmentMangaBookmarksBinding>(),
	OnListItemClickListener<Bookmark> {

	private val activityViewModel by activityViewModels<DetailsViewModel>()
	private val viewModel by viewModels<MangaBookmarksViewModel>()

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private var bookmarksAdapter: BookmarksAdapter? = null
	private var spanResolver: GridSpanResolver? = null

	private val spanSizeLookup = SpanSizeLookup()
	private val listCommitCallback = Runnable {
		spanSizeLookup.invalidateCache()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		activityViewModel.manga.observe(this, viewModel)
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMangaBookmarksBinding {
		return FragmentMangaBookmarksBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: FragmentMangaBookmarksBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		spanResolver = GridSpanResolver(binding.root.resources)
		bookmarksAdapter = BookmarksAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			clickListener = this@MangaBookmarksFragment,
			headerClickListener = null,
		)
		viewModel.gridScale.observe(viewLifecycleOwner, ::onGridScaleChanged) // before rv initialization
		with(binding.recyclerView) {
			addItemDecoration(TypedListSpacingDecoration(context, false))
			adapter = bookmarksAdapter
			addOnLayoutChangeListener(spanResolver)
			(layoutManager as GridLayoutManager).let {
				it.spanSizeLookup = spanSizeLookup
				it.spanCount = checkNotNull(spanResolver).spanCount
			}
		}
		viewModel.content.observe(viewLifecycleOwner, checkNotNull(bookmarksAdapter))
	}

	override fun onDestroyView() {
		spanResolver = null
		bookmarksAdapter = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	override fun onPause() {
		// required for BottomSheetBehavior
		requireViewBinding().recyclerView.isNestedScrollingEnabled = false
		super.onPause()
	}

	override fun onResume() {
		requireViewBinding().recyclerView.isNestedScrollingEnabled = true
		super.onResume()
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun onItemClick(item: Bookmark, view: View) {
		val listener = findParentCallback(ReaderNavigationCallback::class.java)
		if (listener != null && listener.onBookmarkSelected(item)) {
			dismissParentDialog()
		} else {
			val intent = IntentBuilder(view.context)
				.manga(activityViewModel.manga.value ?: return)
				.bookmark(item)
				.incognito(true)
				.build()
			startActivity(intent)
		}
	}

	private fun onGridScaleChanged(scale: Float) {
		spanSizeLookup.invalidateCache()
		spanResolver?.setGridSize(scale, requireViewBinding().recyclerView)
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
}

