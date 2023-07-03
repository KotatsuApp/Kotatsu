package org.koitharu.kotatsu.favourites.ui.categories.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesListListener
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class CategoriesAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	onItemClickListener: FavouriteCategoriesListListener,
	listListener: ListStateHolderListener,
) : AsyncListDifferDelegationAdapter<ListModel>(ListModelDiffCallback) {

	init {
		delegatesManager.addDelegate(categoryAD(coil, lifecycleOwner, onItemClickListener))
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, listListener))
			.addDelegate(loadingStateAD())
	}
}
