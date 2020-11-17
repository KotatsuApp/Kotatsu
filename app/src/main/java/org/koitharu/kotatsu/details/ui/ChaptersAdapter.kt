package org.koitharu.kotatsu.details.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.base.ui.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.base.ui.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.history.domain.ChapterExtra

class ChaptersAdapter(onItemClickListener: OnRecyclerItemClickListener<MangaChapter>) :
	BaseRecyclerAdapter<MangaChapter, ChapterExtra>(onItemClickListener) {

	private val checkedIds = HashSet<Long>()

	val checkedItemsCount: Int
		get() = checkedIds.size

	val checkedItemsIds: Set<Long>
		get() = checkedIds

	var currentChapterId: Long? = null
		set(value) {
			field = value
			updateCurrentPosition()
		}

	var newChaptersCount: Int = 0
		set(value) {
			val updated = maxOf(field, value)
			field = value
			notifyItemRangeChanged(itemCount - updated, updated)
		}

	var currentChapterPosition = RecyclerView.NO_POSITION
		private set

	fun clearChecked() {
		checkedIds.clear()
		notifyDataSetChanged()
	}

	fun checkAll() {
		for (item in dataSet) {
			checkedIds.add(item.id)
		}
		notifyDataSetChanged()
	}

	fun setItemIsChecked(itemId: Long, isChecked: Boolean) {
		if ((isChecked && checkedIds.add(itemId)) || (!isChecked && checkedIds.remove(itemId))) {
			val pos = findItemPositionById(itemId)
			if (pos != RecyclerView.NO_POSITION) {
				notifyItemChanged(pos)
			}
		}
	}

	fun toggleItemChecked(itemId: Long) {
		setItemIsChecked(itemId, itemId !in checkedIds)
	}

	override fun onCreateViewHolder(parent: ViewGroup) = ChapterHolder(parent)

	override fun onGetItemId(item: MangaChapter) = item.id

	override fun getExtra(item: MangaChapter, position: Int): ChapterExtra = when {
		item.id in checkedIds -> ChapterExtra.CHECKED
		currentChapterPosition == RecyclerView.NO_POSITION
				|| currentChapterPosition < position -> if (position >= itemCount - newChaptersCount) {
			ChapterExtra.NEW
		} else {
			ChapterExtra.UNREAD
		}
		currentChapterPosition == position -> ChapterExtra.CURRENT
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