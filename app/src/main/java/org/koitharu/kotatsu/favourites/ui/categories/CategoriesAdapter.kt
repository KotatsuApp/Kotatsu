package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.favourites.ui.categories.adapter.categoryAD
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import kotlin.jvm.internal.Intrinsics

class CategoriesAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	onItemClickListener: OnListItemClickListener<FavouriteCategory>,
	listListener: ListStateHolderListener,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(categoryAD(coil, lifecycleOwner, onItemClickListener))
			.addDelegate(emptyStateListAD(listListener))
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
			return super.getChangePayload(oldItem, newItem)
		}
	}
}