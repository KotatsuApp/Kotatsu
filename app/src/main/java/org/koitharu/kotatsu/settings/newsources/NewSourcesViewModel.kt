package org.koitharu.kotatsu.settings.newsources

import androidx.core.os.LocaleListCompat
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.getLocaleTitle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import org.koitharu.kotatsu.utils.ext.mapToSet

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
		val locales = LocaleListCompat.getDefault().mapToSet { it.language }
		val pendingHidden = HashSet<String>()
		sources.value = initialList.map {
			val locale = it.locale
			val isEnabledByLocale = locale == null || locale in locales
			if (!isEnabledByLocale) {
				pendingHidden += it.name
			}
			SourceConfigItem.SourceItem(
				source = it,
				summary = it.getLocaleTitle(),
				isEnabled = isEnabledByLocale,
				isDraggable = false,
			)
		}
		if (pendingHidden.isNotEmpty()) {
			settings.hiddenSources += pendingHidden
		}
	}
}
