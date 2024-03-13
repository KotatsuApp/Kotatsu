package org.koitharu.kotatsu.details.ui.adapter

import android.content.Context
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.listHeaderAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class ChaptersAdapter(
	private val onItemClickListener: OnListItemClickListener<ChapterListItem>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
		addDelegate(ListItemType.CHAPTER_LIST, chapterListItemAD(onItemClickListener))
		addDelegate(ListItemType.CHAPTER_GRID, chapterGridItemAD(onItemClickListener))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
