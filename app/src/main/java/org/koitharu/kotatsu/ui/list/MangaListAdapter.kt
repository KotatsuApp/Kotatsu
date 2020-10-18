package org.koitharu.kotatsu.ui.list

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.base.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.base.list.OnRecyclerItemClickListener

class MangaListAdapter(onItemClickListener: OnRecyclerItemClickListener<Manga>) :
	BaseRecyclerAdapter<Manga, MangaHistory?>(onItemClickListener) {

	var listMode: ListMode = ListMode.LIST

	override fun onCreateViewHolder(parent: ViewGroup) = when (listMode) {
		ListMode.LIST -> MangaListHolder(parent)
		ListMode.DETAILED_LIST -> MangaListDetailsHolder(
			parent
		)
		ListMode.GRID -> MangaGridHolder(parent)
	}

	override fun onGetItemId(item: Manga) = item.id

	override fun getExtra(item: Manga, position: Int): MangaHistory? = null
}