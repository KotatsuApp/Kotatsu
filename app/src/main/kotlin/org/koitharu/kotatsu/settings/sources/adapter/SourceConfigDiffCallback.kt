package org.koitharu.kotatsu.settings.sources.adapter

import androidx.recyclerview.widget.DiffUtil
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem.EmptySearchResult
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem.Header
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem.LocaleGroup
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem.SourceItem

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

			oldItem is SourceConfigItem.Tip && newItem is SourceConfigItem.Tip -> {
				oldItem.key == newItem.key
			}

			else -> false
		}
	}

	override fun areContentsTheSame(oldItem: SourceConfigItem, newItem: SourceConfigItem): Boolean {
		return oldItem == newItem
	}

	override fun getChangePayload(oldItem: SourceConfigItem, newItem: SourceConfigItem) = Unit
}
