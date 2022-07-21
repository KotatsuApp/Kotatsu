package org.koitharu.kotatsu.main.ui

import android.util.SparseIntArray
import androidx.core.util.set
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.github.AppUpdateRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class MainViewModel(
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings,
	private val appUpdateRepository: AppUpdateRepository,
	private val trackingRepository: TrackingRepository,
) : BaseViewModel() {

	val onOpenReader = SingleLiveEvent<Manga>()

	val isResumeEnabled = historyRepository
		.observeHasItems()
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, false)

	val counters = combine(
		appUpdateRepository.observeAvailableUpdate(),
		trackingRepository.observeUpdatedMangaCount(),
	) { appUpdate, tracks ->
		val a = SparseIntArray(2)
		a[R.id.nav_tools] = if (appUpdate != null) 1 else 0
		a[R.id.nav_feed] = tracks
		a
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, SparseIntArray(0))

	init {
		launchJob(Dispatchers.Default) {
			appUpdateRepository.fetchUpdate()
		}
	}

	fun openLastReader() {
		launchLoadingJob {
			val manga = historyRepository.getLastOrNull() ?: throw EmptyHistoryException()
			onOpenReader.call(manga)
		}
	}
}
