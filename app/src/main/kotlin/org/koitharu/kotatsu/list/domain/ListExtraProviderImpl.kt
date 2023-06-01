package org.koitharu.kotatsu.list.domain

import dagger.Reusable
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import javax.inject.Inject

@Reusable
class ListExtraProviderImpl @Inject constructor(
	private val settings: AppSettings,
	private val trackingRepository: TrackingRepository,
	private val historyRepository: HistoryRepository,
) : ListExtraProvider {

	override suspend fun getCounter(mangaId: Long): Int {
		return if (settings.isTrackerEnabled) {
			trackingRepository.getNewChaptersCount(mangaId)
		} else {
			0
		}
	}

	override suspend fun getProgress(mangaId: Long): Float {
		return if (settings.isReadingIndicatorsEnabled) {
			historyRepository.getProgress(mangaId)
		} else {
			PROGRESS_NONE
		}
	}
}
