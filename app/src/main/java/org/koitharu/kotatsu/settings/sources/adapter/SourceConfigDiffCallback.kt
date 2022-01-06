package org.koitharu.kotatsu.settings.sources.adapter

import androidx.recyclerview.widget.DiffUtil

class SourceConfigDiffCallback : DiffUtil.ItemCallback<SourceConfigItem>() {

	override fun areItemsTheSame(oldItem: SourceConfigItem, newItem: SourceConfigItem): Boolean {
		return when {
			oldItem.javaClass != newItem.javaClass -> false
			oldItem is SourceConfigItem.LocaleHeader && newItem is SourceConfigItem.LocaleHeader -> {
				oldItem.localeId == newItem.localeId
			}
			oldItem is SourceConfigItem.SourceItem && newItem is SourceConfigItem.SourceItem -> {
				oldItem.source == newItem.source
			}
			else -> false
		}
	}

	override fun areContentsTheSame(oldItem: SourceConfigItem, newItem: SourceConfigItem): Boolean {
		return when {
			oldItem is SourceConfigItem.LocaleHeader && newItem is SourceConfigItem.LocaleHeader -> {
				oldItem.title == newItem.title && oldItem.isExpanded == newItem.isExpanded
			}
			oldItem is SourceConfigItem.SourceItem && newItem is SourceConfigItem.SourceItem -> {
				oldItem.isEnabled == newItem.isEnabled
			}
			else -> false
		}
	}

	override fun getChangePayload(oldItem: SourceConfigItem, newItem: SourceConfigItem) = Unit
}