package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem

class MangaCategoriesAdapter(
	clickListener: OnListItemClickListener<MangaCategoryItem>
) : AsyncListDifferDelegationAdapter<MangaCategoryItem>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(mangaCategoryAD(clickListener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<MangaCategoryItem>() {
		override fun areItemsTheSame(
			oldItem: MangaCategoryItem,
			newItem: MangaCategoryItem
		): Boolean = oldItem.id == newItem.id

		override fun areContentsTheSame(
			oldItem: MangaCategoryItem,
			newItem: MangaCategoryItem
		): Boolean = oldItem == newItem

		override fun getChangePayload(
			oldItem: MangaCategoryItem,
			newItem: MangaCategoryItem
		): Any? {
			if (oldItem.isChecked != newItem.isChecked) {
				return newItem.isChecked
			}
			return super.getChangePayload(oldItem, newItem)
		}
	}
}
