package org.koitharu.kotatsu.main.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.base.domain.MangaProviderFactory
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.SingleLiveEvent

class MainViewModel(
	private val historyRepository: HistoryRepository,
	settings: AppSettings
) : BaseViewModel() {

	val onOpenReader = SingleLiveEvent<ReaderState>()
	var defaultSection by settings::defaultSection

	val remoteSources = settings.observe()
		.filter { it == AppSettings.KEY_SOURCES_ORDER || it == AppSettings.KEY_SOURCES_HIDDEN }
		.onStart { emit("") }
		.map { MangaProviderFactory.getSources(settings, includeHidden = false) }
		.distinctUntilChanged()
		.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	fun openLastReader() {
		launchLoadingJob {
			val manga = historyRepository.getList(0, 1).firstOrNull()
				?: throw EmptyHistoryException()
			val history = historyRepository.getOne(manga) ?: throw EmptyHistoryException()
			val state = ReaderState(
				manga.source.repository.getDetails(manga),
				history.chapterId, history.page, history.scroll
			)
			onOpenReader.call(state)
		}
	}
}