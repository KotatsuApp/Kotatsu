package org.koitharu.kotatsu.settings.newsources

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@HiltViewModel
class NewSourcesViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private val newSources = SuspendLazy {
		repository.assimilateNewSources()
	}
	val content: StateFlow<List<SourceConfigItem>> = repository.observeAll()
		.map { sources ->
			val new = newSources.get()
			val skipNsfw = settings.isNsfwContentDisabled
			sources.mapNotNull { (source, enabled) ->
				if (source in new) {
					SourceConfigItem.SourceItem(
						source = source,
						isEnabled = enabled,
						isDraggable = false,
						isAvailable = !skipNsfw || source.contentType != ContentType.HENTAI,
					)
				} else {
					null
				}
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		launchJob(Dispatchers.Default) {
			repository.setSourceEnabled(item.source, isEnabled)
		}
	}
}

