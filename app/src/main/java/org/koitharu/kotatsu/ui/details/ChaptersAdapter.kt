package org.koitharu.kotatsu.ui.details

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.domain.ChapterExtra
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener

class ChaptersAdapter(onItemClickListener: OnRecyclerItemClickListener<MangaChapter>) :
	BaseRecyclerAdapter<MangaChapter, ChapterExtra>(onItemClickListener) {

	var currentChapterPosition = RecyclerView.NO_POSITION
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun onCreateViewHolder(parent: ViewGroup) = ChapterHolder(parent)

	override fun onGetItemId(item: MangaChapter) = item.id

	override fun getExtra(item: MangaChapter, position: Int): ChapterExtra = when {
		currentChapterPosition == RecyclerView.NO_POSITION -> ChapterExtra.UNREAD
		currentChapterPosition == position -> ChapterExtra.CURRENT
		currentChapterPosition < position -> ChapterExtra.UNREAD
		currentChapterPosition > position -> ChapterExtra.READ
		else -> ChapterExtra.UNREAD
	}
}