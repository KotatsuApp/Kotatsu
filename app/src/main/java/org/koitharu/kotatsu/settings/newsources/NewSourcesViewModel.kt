package org.koitharu.kotatsu.settings.newsources

import androidx.lifecycle.MutableLiveData
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class NewSourcesViewModel(
	private val settings: AppSettings,
) : BaseViewModel() {

	val sources = MutableLiveData<List<SourceConfigItem>>()
	private val initialList = settings.newSources

	init {
		buildList()
	}

	fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		if (isEnabled) {
			settings.hiddenSources -= item.source.name
		} else {
			settings.hiddenSources += item.source.name
		}
	}

	fun apply() {
		settings.markKnownSources(initialList)
	}

	private fun buildList() {
		val hidden = settings.hiddenSources
		sources.value = initialList.map {
			SourceConfigItem.SourceItem(
				source = it,
				summary = null,
				isEnabled = it.name !in hidden,
				isDraggable = false,
			)
		}
	}
}