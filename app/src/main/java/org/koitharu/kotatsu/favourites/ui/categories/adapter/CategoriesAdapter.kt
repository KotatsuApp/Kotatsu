package org.koitharu.kotatsu.favourites.ui.categories.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlin.jvm.internal.Intrinsics
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
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(categoryAD(coil, lifecycleOwner, onItemClickListener))
			.addDelegate(emptyStateListAD(coil, listListener))
			.addDelegate(loadingStateAD())
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is CategoryListModel && newItem is CategoryListModel -> {
					oldItem.category.id == newItem.category.id
				}
				else -> oldItem.javaClass == newItem.javaClass
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}

		override fun getChangePayload(oldItem: ListModel, newItem: ListModel): Any? {
			return when {
				oldItem is CategoryListModel && newItem is CategoryListModel -> {
					if (oldItem.category == newItem.category &&
						oldItem.mangaCount == newItem.mangaCount &&
						oldItem.covers == newItem.covers &&
						oldItem.isReorderMode != newItem.isReorderMode
					) {
						Unit
					} else {
						super.getChangePayload(oldItem, newItem)
					}
				}
				else -> super.getChangePayload(oldItem, newItem)
			}
		}
	}
}
