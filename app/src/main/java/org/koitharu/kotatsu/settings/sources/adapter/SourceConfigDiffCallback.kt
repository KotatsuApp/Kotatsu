package org.koitharu.kotatsu.settings.sources.adapter

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem.*

class SourceConfigDiffCallback : DiffUtil.ItemCallback<SourceConfigItem>() {

	override fun areItemsTheSame(oldItem: SourceConfigItem, newItem: SourceConfigItem): Boolean {
		return when {
			oldItem.javaClass != newItem.javaClass -> false
			oldItem is LocaleGroup && newItem is LocaleGroup -> {
				oldItem.localeId == newItem.localeId
			}
			oldItem is SourceItem && newItem is SourceItem -> {
				oldItem.source == newItem.source
			}
			oldItem is Header && newItem is Header -> {
				oldItem.titleResId == newItem.titleResId
			}
			oldItem == EmptySearchResult && newItem == EmptySearchResult -> {
				true
			}
			else -> false
		}
	}

	override fun areContentsTheSame(oldItem: SourceConfigItem, newItem: SourceConfigItem): Boolean {
		return oldItem == newItem
	}

	override fun getChangePayload(oldItem: SourceConfigItem, newItem: SourceConfigItem) = Unit
}