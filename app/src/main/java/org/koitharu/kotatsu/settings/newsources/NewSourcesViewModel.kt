package org.koitharu.kotatsu.settings.newsources

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.getLocaleTitle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

@HiltViewModel
class NewSourcesViewModel @Inject constructor(
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
				summary = it.getLocaleTitle(),
				isEnabled = it.name !in hidden,
				isDraggable = false,
			)
		}
	}
}
