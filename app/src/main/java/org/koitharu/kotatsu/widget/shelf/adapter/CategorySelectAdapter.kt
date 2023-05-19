package org.koitharu.kotatsu.widget.shelf.adapter

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.widget.shelf.model.CategoryItem

class CategorySelectAdapter(
	clickListener: OnListItemClickListener<CategoryItem>
) : AsyncListDifferDelegationAdapter<CategoryItem>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(categorySelectItemAD(clickListener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<CategoryItem>() {

		override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
			return oldItem == newItem
		}

		override fun getChangePayload(oldItem: CategoryItem, newItem: CategoryItem): Any? {
			if (oldItem.isSelected != newItem.isSelected) {
				return newItem.isSelected
			}
			return super.getChangePayload(oldItem, newItem)
		}
	}
}
