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
	chaptersInGridView: Boolean,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		setChapterAdapterDelegate(chaptersInGridView)
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
	}

	fun setChapterAdapterDelegate(chaptersInGridView: Boolean) {
		delegatesManager.removeDelegate(ListItemType.CHAPTER.ordinal)
		if (chaptersInGridView) {
			addDelegate(ListItemType.CHAPTER, chapterGridItemAD(onItemClickListener))
		} else {
			addDelegate(ListItemType.CHAPTER, chapterListItemAD(onItemClickListener))
		}
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
