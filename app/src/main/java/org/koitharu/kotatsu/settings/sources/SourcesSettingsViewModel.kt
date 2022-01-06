package org.koitharu.kotatsu.settings.sources

import androidx.lifecycle.MutableLiveData
import org.koitharu.kotatsu.base.domain.MangaProviderFactory
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigItem
import java.util.*

class SourcesSettingsViewModel(
	private val settings: AppSettings,
) : BaseViewModel() {

	val items = MutableLiveData<List<SourceConfigItem>>(emptyList())
	private val expandedGroups = HashSet<String?>()

	init {
		buildList()
	}

	fun reorderSources(oldPos: Int, newPos: Int) {
		val snapshot = items.value?.toMutableList() ?: return
		Collections.swap(snapshot, oldPos, newPos)
		settings.sourcesOrder = snapshot.mapNotNull {
			(it as? SourceConfigItem.SourceItem)?.source?.ordinal
		}
		buildList()
	}

	fun setEnabled(source: MangaSource, isEnabled: Boolean) {
		settings.hiddenSources = if (isEnabled) {
			settings.hiddenSources - source.name
		} else {
			settings.hiddenSources + source.name
		}
		buildList()
	}

	fun expandOrCollapse(headerId: String?) {
		if (headerId in expandedGroups) {
			expandedGroups.remove(headerId)
		} else {
			expandedGroups.add(headerId)
		}
		buildList()
	}

	private fun buildList() {
		val sources = MangaProviderFactory.getSources(settings, includeHidden = true)
		val hiddenSources = settings.hiddenSources
		items.value = sources.map {
			SourceConfigItem.SourceItem(
				source = it,
				isEnabled = it.name !in hiddenSources,
			)
		}
	}
}