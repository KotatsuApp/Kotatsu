package org.koitharu.kotatsu.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter

open class MangaListAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener,
) : BaseListAdapter() {

	init {
		delegatesManager
			.addDelegate(ITEM_TYPE_MANGA_LIST, mangaListItemAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_MANGA_LIST_DETAILED, mangaListDetailedItemAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_MANGA_GRID, mangaGridItemAD(coil, lifecycleOwner, null, listener))
			.addDelegate(ITEM_TYPE_LOADING_FOOTER, loadingFooterAD())
			.addDelegate(ITEM_TYPE_LOADING_STATE, loadingStateAD())
			.addDelegate(ITEM_TYPE_ERROR_STATE, errorStateListAD(listener))
			.addDelegate(ITEM_TYPE_ERROR_FOOTER, errorFooterAD(listener))
			.addDelegate(ITEM_TYPE_EMPTY, emptyStateListAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_HEADER, listHeaderAD(listener))
	}

	companion object {

		const val ITEM_TYPE_MANGA_LIST = 0
		const val ITEM_TYPE_MANGA_LIST_DETAILED = 1
		const val ITEM_TYPE_MANGA_GRID = 2
		const val ITEM_TYPE_LOADING_FOOTER = 3
		const val ITEM_TYPE_LOADING_STATE = 4
		const val ITEM_TYPE_ERROR_STATE = 6
		const val ITEM_TYPE_ERROR_FOOTER = 7
		const val ITEM_TYPE_EMPTY = 8
		const val ITEM_TYPE_HEADER = 9

		val PAYLOAD_PROGRESS = Any()
	}
}
