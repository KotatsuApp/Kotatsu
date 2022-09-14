package org.koitharu.kotatsu.shelf.ui.config.categories

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory

class ShelfCategoriesConfigAdapter(
	listener: OnListItemClickListener<FavouriteCategory>,
) : AsyncListDifferDelegationAdapter<FavouriteCategory>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(shelfCategoryAD(listener))
	}

	class DiffCallback : DiffUtil.ItemCallback<FavouriteCategory>() {

		override fun areItemsTheSame(oldItem: FavouriteCategory, newItem: FavouriteCategory): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: FavouriteCategory, newItem: FavouriteCategory): Boolean {
			return oldItem.isVisibleInLibrary == newItem.isVisibleInLibrary && oldItem.title == newItem.title
		}

		override fun getChangePayload(oldItem: FavouriteCategory, newItem: FavouriteCategory): Any? {
			return if (oldItem.isVisibleInLibrary == newItem.isVisibleInLibrary) {
				super.getChangePayload(oldItem, newItem)
			} else Unit
		}
	}
}
