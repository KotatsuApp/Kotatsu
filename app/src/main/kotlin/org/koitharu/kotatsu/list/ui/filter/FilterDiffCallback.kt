package org.koitharu.kotatsu.list.ui.filter

import androidx.recyclerview.widget.DiffUtil

class FilterDiffCallback : DiffUtil.ItemCallback<FilterItem>() {

	override fun areItemsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean {
		return when {
			oldItem === newItem -> true
			oldItem.javaClass != newItem.javaClass -> false
			oldItem is FilterItem.Header && newItem is FilterItem.Header -> {
				oldItem.titleResId == newItem.titleResId
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

	override fun areContentsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean {
		return when {
			oldItem == FilterItem.Loading && newItem == FilterItem.Loading -> true
			oldItem is FilterItem.Header && newItem is FilterItem.Header -> {
				oldItem.counter == newItem.counter
			}
			oldItem is FilterItem.Error && newItem is FilterItem.Error -> true
			oldItem is FilterItem.Tag && newItem is FilterItem.Tag -> {
				oldItem.isChecked == newItem.isChecked
			}
			oldItem is FilterItem.Sort && newItem is FilterItem.Sort -> {
				oldItem.isSelected == newItem.isSelected
			}
			else -> false
		}
	}

	override fun getChangePayload(oldItem: FilterItem, newItem: FilterItem): Any? {
		val hasPayload = when {
			oldItem is FilterItem.Tag && newItem is FilterItem.Tag -> {
				oldItem.isChecked != newItem.isChecked
			}
			oldItem is FilterItem.Sort && newItem is FilterItem.Sort -> {
				oldItem.isSelected != newItem.isSelected
			}
			oldItem is FilterItem.Header && newItem is FilterItem.Header -> {
				oldItem.counter != newItem.counter
			}
			else -> false
		}
		return if (hasPayload) Unit else super.getChangePayload(oldItem, newItem)
	}
}