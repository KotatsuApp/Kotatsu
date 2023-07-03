package org.koitharu.kotatsu.main.ui

import android.util.SparseIntArray
import androidx.core.util.set
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.github.AppUpdateRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.main.domain.ReadingResumeEnabledUseCase
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val appUpdateRepository: AppUpdateRepository,
	trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	readingResumeEnabledUseCase: ReadingResumeEnabledUseCase,
) : BaseViewModel() {

	val onOpenReader = MutableEventFlow<Manga>()

	val isResumeEnabled = readingResumeEnabledUseCase().stateIn(
		scope = viewModelScope + Dispatchers.Default,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = false,
	)

	val isFeedAvailable = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_TRACKER_ENABLED,
		valueProducer = { isTrackerEnabled },
	)

	val counters = combine(
		appUpdateRepository.observeAvailableUpdate(),
		trackingRepository.observeUpdatedMangaCount(),
	) { appUpdate, tracks ->
		val a = SparseIntArray(2)
		// a[R.id.nav_tools] = if (appUpdate != null) 1 else 0
		a[R.id.nav_feed] = tracks
		a
	}.stateIn(
		scope = viewModelScope + Dispatchers.Default,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = SparseIntArray(0),
	)

	init {
		launchJob {
			appUpdateRepository.fetchUpdate()
		}
	}

	fun openLastReader() {
		launchLoadingJob(Dispatchers.Default) {
			val manga = historyRepository.getLastOrNull() ?: throw EmptyHistoryException()
			onOpenReader.call(manga)
		}
	}

	fun setIncognitoMode(isEnabled: Boolean) {
		settings.isIncognitoModeEnabled = isEnabled
	}
}
