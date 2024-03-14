package org.koitharu.kotatsu.reader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.findById
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.RecyclerViewScrollCallback
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.databinding.SheetChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.mapChapters
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.pager.chapters.ChapterGridSpanHelper
import org.koitharu.kotatsu.details.ui.withVolumeHeaders
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.parsers.model.MangaChapter
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ChaptersSheet : BaseAdaptiveSheet<SheetChaptersBinding>(),
	OnListItemClickListener<ChapterListItem> {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel: ReaderViewModel by activityViewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = SheetChaptersBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: SheetChaptersBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val manga = viewModel.manga
		if (manga == null) {
			dismissAllowingStateLoss()
			return
		}
		val state = viewModel.getCurrentState()
		val currentChapter = state?.let { manga.allChapters.findById(it.chapterId) }
		val chapters = manga.mapChapters(
			history = state?.let {
				MangaHistory(
					createdAt = Instant.now(),
					updatedAt = Instant.now(),
					chapterId = it.chapterId,
					page = it.page,
					scroll = it.scroll,
					percent = PROGRESS_NONE,
				)
			},
			newCount = 0,
			branch = currentChapter?.branch,
			bookmarks = listOf(),
			isGrid = settings.isChaptersGridView,
		).withVolumeHeaders(binding.root.context)
		if (chapters.isEmpty()) {
			dismissAllowingStateLoss()
			return
		}
		val currentPosition = if (currentChapter != null) {
			chapters.indexOfFirst { it is ChapterListItem && it.chapter.id == currentChapter.id }
		} else {
			-1
		}
		binding.recyclerView.addItemDecoration(TypedListSpacingDecoration(binding.recyclerView.context, true))
		binding.recyclerView.adapter = ChaptersAdapter(this).also { adapter ->
			if (currentPosition >= 0) {
				val targetPosition = (currentPosition - 1).coerceAtLeast(0)
				val offset =
					(resources.getDimensionPixelSize(R.dimen.chapter_list_item_height) * 0.6).roundToInt()
				adapter.setItems(
					chapters, RecyclerViewScrollCallback(binding.recyclerView, targetPosition, offset),
				)
			} else {
				adapter.items = chapters
			}
		}
		ChapterGridSpanHelper.attach(binding.recyclerView)
		binding.recyclerView.layoutManager = if (settings.isChaptersGridView) {
			GridLayoutManager(context, ChapterGridSpanHelper.getSpanCount(binding.recyclerView)).apply {
				spanSizeLookup = ChapterGridSpanHelper.SpanSizeLookup(binding.recyclerView)
			}
		} else {
			LinearLayoutManager(context)
		}
	}

	override fun onItemClick(item: ChapterListItem, view: View) {
		((parentFragment as? OnChapterChangeListener)
			?: (activity as? OnChapterChangeListener))?.let {
			dismiss()
			it.onChapterChanged(item.chapter)
		}
	}

	fun interface OnChapterChangeListener {

		fun onChapterChanged(chapter: MangaChapter)
	}

	companion object {

		private const val TAG = "ChaptersBottomSheet"

		fun show(fm: FragmentManager) = ChaptersSheet().showDistinct(fm, TAG)
	}
}
