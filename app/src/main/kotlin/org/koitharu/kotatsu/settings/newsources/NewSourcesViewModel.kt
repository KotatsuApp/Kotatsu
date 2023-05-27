package org.koitharu.kotatsu.settings.newsources

import androidx.core.os.LocaleListCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.model.getLocaleTitle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.mapToSet
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@HiltViewModel
class NewSourcesViewModel @Inject constructor(
	private val settings: AppSettings,
) : BaseViewModel() {

	val sources = MutableStateFlow<List<SourceConfigItem>>(buildList())
	private val initialList = settings.newSources

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

	private fun buildList(): List<SourceConfigItem.SourceItem> {
		val locales = LocaleListCompat.getDefault().mapToSet { it.language }
		val pendingHidden = HashSet<String>()
		return initialList.map {
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
		}.also {
			if (pendingHidden.isNotEmpty()) {
				settings.hiddenSources += pendingHidden
			}
		}
	}
}
