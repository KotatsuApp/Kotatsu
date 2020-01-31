package org.koitharu.kotatsu.ui.main.list

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter

class MangaListAdapter(onItemClickListener: ((Manga) -> Unit)?) :
	BaseRecyclerAdapter<Manga>(onItemClickListener) {

	override fun onCreateViewHolder(parent: ViewGroup) = MangaListHolder(parent)

	override fun onGetItemId(item: Manga) = item.id
}