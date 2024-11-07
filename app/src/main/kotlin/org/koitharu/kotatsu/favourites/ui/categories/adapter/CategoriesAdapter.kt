package org.koitharu.kotatsu.favourites.ui.categories.adapter

import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import org.koitharu.kotatsu.core.ui.ReorderableListAdapter
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesListListener
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class CategoriesAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	onItemClickListener: FavouriteCategoriesListListener,
	listListener: ListStateHolderListener,
) : ReorderableListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.CATEGORY_LARGE, categoryAD(coil, lifecycleOwner, onItemClickListener))
		addDelegate(ListItemType.NAV_ITEM, allCategoriesAD(coil, lifecycleOwner, onItemClickListener))
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(coil, lifecycleOwner, listListener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}
}
