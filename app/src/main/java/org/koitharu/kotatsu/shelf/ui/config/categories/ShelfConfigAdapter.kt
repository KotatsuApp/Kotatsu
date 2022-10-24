package org.koitharu.kotatsu.shelf.ui.config.categories

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener

class ShelfConfigAdapter(
	listener: OnListItemClickListener<ShelfConfigModel>,
) : AsyncListDifferDelegationAdapter<ShelfConfigModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(shelfCategoryAD(listener))
			.addDelegate(shelfSectionAD(listener))
	}

	class DiffCallback : DiffUtil.ItemCallback<ShelfConfigModel>() {

		override fun areItemsTheSame(oldItem: ShelfConfigModel, newItem: ShelfConfigModel): Boolean {
			return when {
				oldItem is ShelfConfigModel.Section && newItem is ShelfConfigModel.Section -> {
					oldItem.section == newItem.section
				}

				oldItem is ShelfConfigModel.FavouriteCategory && newItem is ShelfConfigModel.FavouriteCategory -> {
					oldItem.id == newItem.id
				}

				else -> false
			}
		}

		override fun areContentsTheSame(oldItem: ShelfConfigModel, newItem: ShelfConfigModel): Boolean {
			return oldItem == newItem
		}

		override fun getChangePayload(oldItem: ShelfConfigModel, newItem: ShelfConfigModel): Any? {
			return if (oldItem.isChecked == newItem.isChecked) {
				super.getChangePayload(oldItem, newItem)
			} else Unit
		}
	}
}
