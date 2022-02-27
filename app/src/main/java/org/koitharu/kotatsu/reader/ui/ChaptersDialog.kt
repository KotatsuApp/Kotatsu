package org.koitharu.kotatsu.reader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.DialogChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.utils.ext.withArgs

class ChaptersDialog : AlertDialogFragment<DialogChaptersBinding>(),
	OnListItemClickListener<ChapterListItem> {

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogChaptersBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder) {
		builder.setTitle(R.string.chapters)
			.setNegativeButton(R.string.close, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.recyclerViewChapters.addItemDecoration(
			MaterialDividerItemDecoration(view.context, RecyclerView.VERTICAL)
		)
		val chapters = arguments?.getParcelableArrayList<MangaChapter>(ARG_CHAPTERS)
		if (chapters == null) {
			dismissAllowingStateLoss()
			return
		}
		val currentId = arguments?.getLong(ARG_CURRENT_ID, 0L) ?: 0L
		val currentPosition = chapters.indexOfFirst { it.id == currentId }
		val dateFormat = get<AppSettings>().getDateFormat()
		binding.recyclerViewChapters.adapter = ChaptersAdapter(this).apply {
			setItems(chapters.mapIndexed { index, chapter ->
				chapter.toListItem(
					isCurrent = index == currentPosition,
					isUnread = index > currentPosition,
					isNew = false,
					isMissing = false,
					isDownloaded = false,
					dateFormat = dateFormat,
				)
			}) {
				if (currentPosition >= 0) {
					with(binding.recyclerViewChapters) {
						(layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
							currentPosition,
							height / 3
						)
					}
				}
			}
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

		private const val TAG = "ChaptersDialog"

		private const val ARG_CHAPTERS = "chapters"
		private const val ARG_CURRENT_ID = "current_id"

		fun show(fm: FragmentManager, chapters: List<MangaChapter>, currentId: Long = 0L) =
			ChaptersDialog().withArgs(2) {
				putParcelableArrayList(ARG_CHAPTERS, ArrayList(chapters))
				putLong(ARG_CURRENT_ID, currentId)
			}.show(fm, TAG)
	}
}
