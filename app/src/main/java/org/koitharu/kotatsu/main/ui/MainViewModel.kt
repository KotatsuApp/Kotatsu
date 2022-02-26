package org.koitharu.kotatsu.main.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class MainViewModel(
	private val historyRepository: HistoryRepository,
	settings: AppSettings
) : BaseViewModel() {

	val onOpenReader = SingleLiveEvent<Manga>()
	var defaultSection by settings::defaultSection

	val remoteSources = settings.observe()
		.filter { it == AppSettings.KEY_SOURCES_ORDER || it == AppSettings.KEY_SOURCES_HIDDEN }
		.onStart { emit("") }
		.map { settings.getMangaSources(includeHidden = false) }
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	fun openLastReader() {
		launchLoadingJob {
			val manga = historyRepository.getList(0, 1).firstOrNull()
				?: throw EmptyHistoryException()
			onOpenReader.call(manga)
		}
	}
}
