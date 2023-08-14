package org.koitharu.kotatsu.favourites.ui.categories.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesListListener
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class CategoriesAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	onItemClickListener: FavouriteCategoriesListListener,
	listListener: ListStateHolderListener,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(categoryAD(coil, lifecycleOwner, onItemClickListener))
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, listListener))
			.addDelegate(loadingStateAD())
	}
}
