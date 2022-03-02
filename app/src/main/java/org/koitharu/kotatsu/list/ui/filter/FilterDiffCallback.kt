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
			else -> false
		}
	}

	override fun areContentsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean {
		return when {
			oldItem === newItem -> true
			oldItem is FilterItem.Header && newItem is FilterItem.Header -> true
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
		val isCheckedChanged = when {
			oldItem is FilterItem.Tag && newItem is FilterItem.Tag -> {
				oldItem.isChecked != newItem.isChecked
			}
			oldItem is FilterItem.Sort && newItem is FilterItem.Sort -> {
				oldItem.isSelected != newItem.isSelected
			}
			else -> false
		}
		return if (isCheckedChanged) Unit else super.getChangePayload(oldItem, newItem)
	}
}