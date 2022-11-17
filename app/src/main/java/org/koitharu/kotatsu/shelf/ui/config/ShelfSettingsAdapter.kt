package org.koitharu.kotatsu.shelf.ui.config

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter

class ShelfSettingsAdapter(
	listener: ShelfSettingsListener,
) : AsyncListDifferDelegationAdapter<ShelfSettingsItemModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(shelfCategoryAD(listener))
			.addDelegate(shelfSectionAD(listener))
	}

	class DiffCallback : DiffUtil.ItemCallback<ShelfSettingsItemModel>() {

		override fun areItemsTheSame(oldItem: ShelfSettingsItemModel, newItem: ShelfSettingsItemModel): Boolean {
			return when {
				oldItem is ShelfSettingsItemModel.Section && newItem is ShelfSettingsItemModel.Section -> {
					oldItem.section == newItem.section
				}

				oldItem is ShelfSettingsItemModel.FavouriteCategory && newItem is ShelfSettingsItemModel.FavouriteCategory -> {
					oldItem.id == newItem.id
				}

				else -> false
			}
		}

		override fun areContentsTheSame(oldItem: ShelfSettingsItemModel, newItem: ShelfSettingsItemModel): Boolean {
			return oldItem == newItem
		}

		override fun getChangePayload(oldItem: ShelfSettingsItemModel, newItem: ShelfSettingsItemModel): Any? {
			return if (oldItem.isChecked == newItem.isChecked) {
				super.getChangePayload(oldItem, newItem)
			} else Unit
		}
	}
}
