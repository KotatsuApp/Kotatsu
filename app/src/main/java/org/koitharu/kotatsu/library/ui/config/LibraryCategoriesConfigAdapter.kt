package org.koitharu.kotatsu.library.ui.config

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory

class LibraryCategoriesConfigAdapter(
	listener: OnListItemClickListener<FavouriteCategory>,
) : AsyncListDifferDelegationAdapter<FavouriteCategory>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(libraryCategoryAD(listener))
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