package org.koitharu.kotatsu.ui.reader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_chapters.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.common.AlertDialogFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.details.ChaptersAdapter
import org.koitharu.kotatsu.utils.ext.withArgs

class ChaptersDialog : AlertDialogFragment(R.layout.dialog_chapters),
	OnRecyclerItemClickListener<MangaChapter> {

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setTitle(R.string.chapters)
			.setNegativeButton(R.string.close, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		recyclerView_chapters.addItemDecoration(
			DividerItemDecoration(
				requireContext(),
				RecyclerView.VERTICAL
			)
		)
		recyclerView_chapters.adapter = ChaptersAdapter(this).apply {
			arguments?.getParcelableArrayList<MangaChapter>(ARG_CHAPTERS)?.let(this::replaceData)
			currentChapterId = arguments?.getLong(ARG_CURRENT_ID, 0L)?.takeUnless { it == 0L }
		}
	}

	override fun onResume() {
		super.onResume()
		val pos = (recyclerView_chapters.adapter as? ChaptersAdapter)?.currentChapterPosition
			?: RecyclerView.NO_POSITION
		if (pos != RecyclerView.NO_POSITION) {
			(recyclerView_chapters.layoutManager as? LinearLayoutManager)
				?.scrollToPositionWithOffset(pos, 100)
		}
	}

	override fun onItemClick(item: MangaChapter, position: Int, view: View) {
		((parentFragment as? OnChapterChangeListener)
			?: (activity as? OnChapterChangeListener))?.let {
			dismiss()
			it.onChapterChanged(item)
		}
	}

	interface OnChapterChangeListener {

		fun onChapterChanged(chapter: MangaChapter)
	}

	companion object {

		private const val TAG = "ChaptersDialog"

		private const val ARG_CHAPTERS = "chapters"
		private const val ARG_CURRENT_ID = "current_id"

		fun show(fm: FragmentManager, chapters: List<MangaChapter>, currentId: Long = 0L) =
			ChaptersDialog()
				.withArgs(2) {
					putParcelableArrayList(ARG_CHAPTERS, ArrayList(chapters))
					putLong(ARG_CURRENT_ID, currentId)
				}.show(fm, TAG)
	}
}