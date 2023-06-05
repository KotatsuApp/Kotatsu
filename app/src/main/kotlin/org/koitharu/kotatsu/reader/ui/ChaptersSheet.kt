package org.koitharu.kotatsu.reader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaChapters
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.RecyclerViewScrollCallback
import org.koitharu.kotatsu.core.util.ext.getParcelableCompat
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.parsers.model.MangaChapter
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ChaptersSheet : BaseAdaptiveSheet<SheetChaptersBinding>(), OnListItemClickListener<ChapterListItem> {

	@Inject
	lateinit var settings: AppSettings

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetChaptersBinding {
		return SheetChaptersBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetChaptersBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val chapters = arguments?.getParcelableCompat<ParcelableMangaChapters>(ARG_CHAPTERS)?.chapters
		if (chapters.isNullOrEmpty()) {
			dismissAllowingStateLoss()
			return
		}
		val currentId = requireArguments().getLong(ARG_CURRENT_ID, 0L)
		val currentPosition = chapters.indexOfFirst { it.id == currentId }
		val items = chapters.mapIndexed { index, chapter ->
			chapter.toListItem(
				isCurrent = index == currentPosition,
				isUnread = index > currentPosition,
				isNew = false,
				isDownloaded = false,
				isBookmarked = false,
			)
		}
		binding.recyclerView.adapter = ChaptersAdapter(this).also { adapter ->
			if (currentPosition >= 0) {
				val targetPosition = (currentPosition - 1).coerceAtLeast(0)
				val offset = (resources.getDimensionPixelSize(R.dimen.chapter_list_item_height) * 0.6).roundToInt()
				adapter.setItems(items, RecyclerViewScrollCallback(binding.recyclerView, targetPosition, offset))
			} else {
				adapter.items = items
			}
		}
	}

	override fun onItemClick(item: ChapterListItem, view: View) {
		((parentFragment as? OnChapterChangeListener) ?: (activity as? OnChapterChangeListener))?.let {
			dismiss()
			it.onChapterChanged(item.chapter)
		}
	}

	fun interface OnChapterChangeListener {

		fun onChapterChanged(chapter: MangaChapter)
	}

	companion object {

		private const val ARG_CHAPTERS = "chapters"
		private const val ARG_CURRENT_ID = "current_id"

		private const val TAG = "ChaptersBottomSheet"

		fun show(
			fm: FragmentManager,
			chapters: List<MangaChapter>,
			currentId: Long,
		) = ChaptersSheet().withArgs(2) {
			putParcelable(ARG_CHAPTERS, ParcelableMangaChapters(chapters))
			putLong(ARG_CURRENT_ID, currentId)
		}.show(fm, TAG)
	}
}
