package org.koitharu.kotatsu.tracker.domain

import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.domain.model.MangaTracking
import org.koitharu.kotatsu.tracker.domain.model.MangaUpdates
import org.koitharu.kotatsu.tracker.work.TrackerNotificationChannels
import org.koitharu.kotatsu.tracker.work.TrackingItem

class Tracker @Inject constructor(
	private val settings: AppSettings,
	private val repository: TrackingRepository,
	private val channels: TrackerNotificationChannels,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend fun getAllTracks(): List<TrackingItem> {
		val sources = settings.trackSources
		if (sources.isEmpty()) {
			return emptyList()
		}
		val knownIds = HashSet<Manga>()
		val result = ArrayList<TrackingItem>()
		// Favourites
		if (AppSettings.TRACK_FAVOURITES in sources) {
			val favourites = repository.getAllFavouritesManga()
			channels.updateChannels(favourites.keys)
			for ((category, mangaList) in favourites) {
				if (!category.isTrackingEnabled || mangaList.isEmpty()) {
					continue
				}
				val categoryTracks = repository.getTracks(mangaList)
				val channelId = if (channels.isFavouriteNotificationsEnabled(category)) {
					channels.getFavouritesChannelId(category.id)
				} else {
					null
				}
				for (track in categoryTracks) {
					if (knownIds.add(track.manga)) {
						result.add(TrackingItem(track, channelId))
					}
				}
			}
		}
		// History
		if (AppSettings.TRACK_HISTORY in sources) {
			val history = repository.getAllHistoryManga()
			val historyTracks = repository.getTracks(history)
			val channelId = if (channels.isHistoryNotificationsEnabled()) {
				channels.getHistoryChannelId()
			} else {
				null
			}
			for (track in historyTracks) {
				if (knownIds.add(track.manga)) {
					result.add(TrackingItem(track, channelId))
				}
			}
		}
		result.trimToSize()
		return result
	}

	suspend fun gc() {
		repository.gc()
	}

	suspend fun fetchUpdates(track: MangaTracking, commit: Boolean): MangaUpdates {
		val manga = mangaRepositoryFactory.create(track.manga.source).getDetails(track.manga)
		val updates = compare(track, manga)
		if (commit) {
			repository.saveUpdates(updates)
		}
		return updates
	}

	@VisibleForTesting
	suspend fun checkUpdates(manga: Manga, commit: Boolean): MangaUpdates {
		val track = repository.getTrack(manga)
		val updates = compare(track, manga)
		if (commit) {
			repository.saveUpdates(updates)
		}
		return updates
	}

	@VisibleForTesting
	suspend fun deleteTrack(mangaId: Long) {
		repository.deleteTrack(mangaId)
	}

	/**
	 * The main functionality of tracker: check new chapters in [manga] comparing to the [track]
	 */
	private fun compare(track: MangaTracking, manga: Manga): MangaUpdates {
		if (track.isEmpty()) {
			// first check or manga was empty on last check
			return MangaUpdates(manga, emptyList(), isValid = false)
		}
		val chapters = requireNotNull(manga.chapters)
		val newChapters = chapters.takeLastWhile { x -> x.id != track.lastChapterId }
		return when {
			newChapters.isEmpty() -> {
				MangaUpdates(manga, emptyList(), isValid = chapters.lastOrNull()?.id == track.lastChapterId)
			}
			newChapters.size == chapters.size -> {
				MangaUpdates(manga, emptyList(), isValid = false)
			}
			else -> {
				MangaUpdates(manga, newChapters, isValid = true)
			}
		}
	}
}
