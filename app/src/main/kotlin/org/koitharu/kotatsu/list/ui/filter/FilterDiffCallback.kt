package org.koitharu.kotatsu.list.ui.filter

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel

class FilterDiffCallback : DiffUtil.ItemCallback<ListModel>() {

	override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
		return when {
			oldItem === newItem -> true
			oldItem.javaClass != newItem.javaClass -> false
			oldItem is ListHeader && newItem is ListHeader -> {
				oldItem == newItem
			}

			oldItem is FilterItem.Tag && newItem is FilterItem.Tag -> {
				oldItem.tag == newItem.tag
			}

			oldItem is FilterItem.Sort && newItem is FilterItem.Sort -> {
				oldItem.order == newItem.order
			}

			oldItem is FilterItem.Error && newItem is FilterItem.Error -> {
				oldItem.textResId == newItem.textResId
			}

			else -> false
		}
	}

	override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
		return oldItem == newItem
	}

	override fun getChangePayload(oldItem: ListModel, newItem: ListModel): Any? {
		val hasPayload = when {
			oldItem is FilterItem.Tag && newItem is FilterItem.Tag -> {
				oldItem.isChecked != newItem.isChecked
			}

			oldItem is FilterItem.Sort && newItem is FilterItem.Sort -> {
				oldItem.isSelected != newItem.isSelected
			}

			else -> false
		}
		return if (hasPayload) Unit else super.getChangePayload(oldItem, newItem)
	}
}
