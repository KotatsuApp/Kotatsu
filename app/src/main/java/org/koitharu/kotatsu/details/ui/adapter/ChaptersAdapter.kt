package org.koitharu.kotatsu.details.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import kotlin.jvm.internal.Intrinsics

class ChaptersAdapter(
	onItemClickListener: OnListItemClickListener<MangaChapter>
) :	AsyncListDifferDelegationAdapter<ChapterListItem>(DiffCallback()) {

	init {
		setHasStableIds(true)
		delegatesManager.addDelegate(chapterListItemAD(onItemClickListener))
	}

	override fun getItemId(position: Int): Long {
		return items[position].chapter.id
	}

	private class DiffCallback : DiffUtil.ItemCallback<ChapterListItem>() {

		override fun areItemsTheSame(oldItem: ChapterListItem, newItem: ChapterListItem): Boolean {
			return oldItem.chapter.id == newItem.chapter.id
		}

		override fun areContentsTheSame(oldItem: ChapterListItem, newItem: ChapterListItem): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}

		override fun getChangePayload(oldItem: ChapterListItem, newItem: ChapterListItem): Any? {
			if (oldItem.extra != newItem.extra) {
				return newItem.extra
			}
			return null
		}
	}
}