package org.koitharu.kotatsu.main.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.prefs.AppSection
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsLiveData
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class MainViewModel(
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	val onOpenReader = SingleLiveEvent<Manga>()
	var defaultSection: AppSection
		get() = settings.defaultSection
		set(value) {
			settings.defaultSection = value
		}

	val isSuggestionsEnabled = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_SUGGESTIONS,
		valueProducer = { isSuggestionsEnabled },
	)

	val isTrackerEnabled = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_TRACKER_ENABLED,
		valueProducer = { isTrackerEnabled },
	)

	val isResumeEnabled = historyRepository
		.observeHasItems()
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, false)

	fun openLastReader() {
		launchLoadingJob {
			val manga = historyRepository.getLastOrNull() ?: throw EmptyHistoryException()
			onOpenReader.call(manga)
		}
	}
}