package org.koitharu.kotatsu.reader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaChapters
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.SheetChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.utils.BottomSheetToolbarController
import org.koitharu.kotatsu.utils.RecyclerViewScrollCallback
import org.koitharu.kotatsu.utils.ext.withArgs

class ChaptersBottomSheet : BaseBottomSheet<SheetChaptersBinding>(), OnListItemClickListener<ChapterListItem> {

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetChaptersBinding {
		return SheetChaptersBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		behavior?.addBottomSheetCallback(BottomSheetToolbarController(binding.toolbar))
		if (!resources.getBoolean(R.bool.is_tablet)) {
			binding.toolbar.navigationIcon = null
		}
		val chapters = arguments?.getParcelable<ParcelableMangaChapters>(ARG_CHAPTERS)?.chapters
		if (chapters.isNullOrEmpty()) {
			dismissAllowingStateLoss()
			return
		}
		val currentId = requireArguments().getLong(ARG_CURRENT_ID, 0L)
		val currentPosition = chapters.indexOfFirst { it.id == currentId }
		val dateFormat = get<AppSettings>().getDateFormat()
		val items = chapters.mapIndexed { index, chapter ->
			chapter.toListItem(
				isCurrent = index == currentPosition,
				isUnread = index > currentPosition,
				isNew = false,
				isMissing = false,
				isDownloaded = false,
				dateFormat = dateFormat,
			)
		}
		binding.recyclerView.adapter = ChaptersAdapter(this).also { adapter ->
			if (currentPosition >= 0) {
				val targetPosition = (currentPosition - 1).coerceAtLeast(0)
				adapter.setItems(items, RecyclerViewScrollCallback(binding.recyclerView, targetPosition))
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
		) = ChaptersBottomSheet().withArgs(2) {
			putParcelable(ARG_CHAPTERS, ParcelableMangaChapters(chapters))
			putLong(ARG_CURRENT_ID, currentId)
		}.show(fm, TAG)

		private fun <T> List<T>.asArrayList(): ArrayList<T> {
			return this as? ArrayList<T> ?: ArrayList(this)
		}
	}
}