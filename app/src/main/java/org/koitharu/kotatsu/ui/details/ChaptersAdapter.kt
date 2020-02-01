package org.koitharu.kotatsu.ui.details

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener

class ChaptersAdapter(onItemClickListener: OnRecyclerItemClickListener<MangaChapter>) :
	BaseRecyclerAdapter<MangaChapter>(onItemClickListener) {

	override fun onCreateViewHolder(parent: ViewGroup) = ChapterHolder(parent)

	override fun onGetItemId(item: MangaChapter) = item.id
}