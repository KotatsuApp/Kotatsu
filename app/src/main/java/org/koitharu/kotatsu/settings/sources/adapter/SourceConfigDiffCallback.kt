package org.koitharu.kotatsu.settings.sources.adapter

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourceConfigDiffCallback : DiffUtil.ItemCallback<SourceConfigItem>() {

	override fun areItemsTheSame(oldItem: SourceConfigItem, newItem: SourceConfigItem): Boolean {
		return when {
			oldItem.javaClass != newItem.javaClass -> false
			oldItem is SourceConfigItem.LocaleGroup && newItem is SourceConfigItem.LocaleGroup -> {
				oldItem.localeId == newItem.localeId
			}
			oldItem is SourceConfigItem.SourceItem && newItem is SourceConfigItem.SourceItem -> {
				oldItem.source == newItem.source
			}
			oldItem is SourceConfigItem.Header && newItem is SourceConfigItem.Header -> {
				oldItem.titleResId == newItem.titleResId
			}
			else -> false
		}
	}

	override fun areContentsTheSame(oldItem: SourceConfigItem, newItem: SourceConfigItem): Boolean {
		return oldItem == newItem
	}

	override fun getChangePayload(oldItem: SourceConfigItem, newItem: SourceConfigItem) = Unit
}