package org.koitharu.kotatsu.ui.details

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.domain.history.ChapterExtra
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener

class ChaptersAdapter(onItemClickListener: OnRecyclerItemClickListener<MangaChapter>) :
	BaseRecyclerAdapter<MangaChapter, ChapterExtra>(onItemClickListener) {

	var currentChapterId: Long? = null
		set(value) {
			field = value
			updateCurrentPosition()
		}

	var currentChapterPosition = RecyclerView.NO_POSITION
		private set

	override fun onCreateViewHolder(parent: ViewGroup) = ChapterHolder(parent)

	override fun onGetItemId(item: MangaChapter) = item.id

	override fun getExtra(item: MangaChapter, position: Int): ChapterExtra = when {
		currentChapterPosition == RecyclerView.NO_POSITION -> ChapterExtra.UNREAD
		currentChapterPosition == position -> ChapterExtra.CURRENT
		currentChapterPosition < position -> ChapterExtra.UNREAD
		currentChapterPosition > position -> ChapterExtra.READ
		else -> ChapterExtra.UNREAD
	}

	override fun onDataSetChanged() {
		super.onDataSetChanged()
		updateCurrentPosition()
	}

	private fun updateCurrentPosition() {
		val pos = currentChapterId?.let {
			dataSet.indexOfFirst { x -> x.id == it }
		} ?: RecyclerView.NO_POSITION
		if (pos != currentChapterPosition) {
			currentChapterPosition = pos
			notifyDataSetChanged()
		}
	}
}