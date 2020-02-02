package org.koitharu.kotatsu.ui.main.list

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaInfo
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener

class MangaListAdapter<E>(onItemClickListener: OnRecyclerItemClickListener<MangaInfo<E>>) :
	BaseRecyclerAdapter<MangaInfo<E>>(onItemClickListener) {

	var listMode: ListMode = ListMode.LIST

	override fun onCreateViewHolder(parent: ViewGroup) = when(listMode) {
		ListMode.LIST -> MangaListHolder<E>(parent)
		ListMode.DETAILED_LIST -> MangaListDetailsHolder<E>(parent)
		ListMode.GRID -> MangaGridHolder(parent)
	}

	override fun onGetItemId(item: MangaInfo<E>) = item.manga.id
}