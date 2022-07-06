package org.koitharu.kotatsu.explore.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.explore.ui.model.ExploreItem

class ExploreDiffCallback : DiffUtil.ItemCallback<ExploreItem>() {

	override fun areItemsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
		return when {
			oldItem.javaClass != newItem.javaClass -> false
			oldItem is ExploreItem.Buttons && newItem is ExploreItem.Buttons -> {
				oldItem == newItem
			}
			oldItem is ExploreItem.Source && newItem is ExploreItem.Source -> {
				oldItem.source == newItem.source
			}
			oldItem is ExploreItem.Header && newItem is ExploreItem.Header -> {
				oldItem.titleResId == newItem.titleResId
			}
			else -> false
		}
	}

	override fun areContentsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
		return oldItem == newItem
	}

}