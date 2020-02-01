package org.koitharu.kotatsu.ui.main.details

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class ChaptersAdapter(onItemClickListener: ((MangaChapter) -> Unit)?) :
	BaseRecyclerAdapter<MangaChapter>(onItemClickListener) {

	override fun onCreateViewHolder(parent: ViewGroup) = ChapterHolder(parent)

	override fun onGetItemId(item: MangaChapter) = item.id
}