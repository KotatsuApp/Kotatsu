package org.koitharu.kotatsu.settings.sources.adapter

import org.koitharu.kotatsu.base.ui.list.OnTipCloseListener
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

interface SourceConfigListener : OnTipCloseListener<SourceConfigItem.Tip> {

	fun onItemSettingsClick(item: SourceConfigItem.SourceItem)

	fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean)

	fun onHeaderClick(header: SourceConfigItem.LocaleGroup)
}
