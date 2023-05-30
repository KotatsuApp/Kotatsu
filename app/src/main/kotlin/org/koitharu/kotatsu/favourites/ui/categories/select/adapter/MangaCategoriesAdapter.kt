package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.favourites.ui.categories.select.model.CategoriesHeaderItem
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.list.ui.model.ListModel

class MangaCategoriesAdapter(
	clickListener: OnListItemClickListener<MangaCategoryItem>,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(mangaCategoryAD(clickListener))
			.addDelegate(categoriesHeaderAD())
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {
		override fun areItemsTheSame(
			oldItem: ListModel,
			newItem: ListModel,
		): Boolean = when {
			oldItem is MangaCategoryItem && newItem is MangaCategoryItem -> oldItem.id == newItem.id
			oldItem is CategoriesHeaderItem && newItem is CategoriesHeaderItem -> oldItem == newItem
			else -> false
		}

		override fun areContentsTheSame(
			oldItem: ListModel,
			newItem: ListModel,
		): Boolean = oldItem == newItem

		override fun getChangePayload(
			oldItem: ListModel,
			newItem: ListModel,
		): Any? {
			if (oldItem is MangaCategoryItem && newItem is MangaCategoryItem && oldItem.isChecked != newItem.isChecked) {
				return newItem.isChecked
			}
			return super.getChangePayload(oldItem, newItem)
		}
	}
}
