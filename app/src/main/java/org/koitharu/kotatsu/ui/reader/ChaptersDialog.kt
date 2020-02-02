package org.koitharu.kotatsu.ui.reader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_chapters.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.common.AlertDialogFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.details.ChaptersAdapter
import org.koitharu.kotatsu.utils.ext.withArgs

class ChaptersDialog : AlertDialogFragment(R.layout.dialog_chapters), OnRecyclerItemClickListener<MangaChapter> {

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setTitle(R.string.chapters)
			.setNegativeButton(R.string.close, null)
			.setCancelable(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		recyclerView_chapters.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
		recyclerView_chapters.adapter = ChaptersAdapter(this).apply {
			arguments?.getParcelableArrayList<MangaChapter>(ARG_CHAPTERS)?.let(this::replaceData)
		}
	}

	override fun onItemClick(item: MangaChapter, position: Int, view: View) {

	}

	companion object {

		private const val TAG = "ChaptersDialog"

		private const val ARG_CHAPTERS = "chapters"

		fun show(fm: FragmentManager, chapters: List<MangaChapter>) = ChaptersDialog()
			.withArgs(1) {
				putParcelableArrayList(ARG_CHAPTERS, ArrayList(chapters))
			}.show(fm, TAG)
	}
}