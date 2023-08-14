package org.koitharu.kotatsu.details.ui.adapter

import android.content.Context
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.details.ui.model.ChapterListItem

class ChaptersAdapter(
	onItemClickListener: OnListItemClickListener<ChapterListItem>,
) : BaseListAdapter<ChapterListItem>(), FastScroller.SectionIndexer {

	init {
		setHasStableIds(true)
		delegatesManager.addDelegate(chapterListItemAD(onItemClickListener))
	}

	override fun getItemId(position: Int): Long {
		return items[position].chapter.id
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		val item = items.getOrNull(position) ?: return null
		return item.chapter.number.toString()
	}
}
