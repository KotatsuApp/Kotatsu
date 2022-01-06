package org.koitharu.kotatsu.settings.sources.adapter

import androidx.recyclerview.widget.RecyclerView

interface SourceConfigListener {

	fun onItemSettingsClick(item: SourceConfigItem.SourceItem)

	fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean)

	fun onDragHandleTouch(holder: RecyclerView.ViewHolder)

	fun onHeaderClick(header: SourceConfigItem.LocaleHeader)
}