package org.koitharu.kotatsu.tracker.domain

import org.koitharu.kotatsu.core.model.MangaTracking
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.tracker.domain.model.MangaUpdates

class Tracker(
	private val repository: TrackingRepository,
) {

	suspend fun fetchUpdates(track: MangaTracking, commit: Boolean): MangaUpdates {
		val repo = MangaRepository(track.manga.source)
		val details = repo.getDetails(track.manga)
		val chapters = details.chapters.orEmpty()
		if (track.isEmpty()) {
			// first check or manga was empty on last check
			if (commit) {
				repository.storeTrackResult(
					mangaId = track.manga.id,
					knownChaptersCount = chapters.size,
					lastChapterId = chapters.lastOrNull()?.id ?: 0L,
					previousTrackChapterId = 0L,
					newChapters = emptyList(),
					saveTrackLog = false,
				)
			}
			return MangaUpdates(
				manga = details,
				newChapters = emptyList(),
			)
		}
		val newChapters = details.getNewChapters(track.lastChapterId)
		if (newChapters.isEmpty()) {
			if (commit) {
				repository.storeTrackResult(
					mangaId = track.manga.id,
					knownChaptersCount = chapters.size,
					lastChapterId = chapters.lastOrNull()?.id ?: 0L,
					previousTrackChapterId = 0L,
					newChapters = emptyList(),
					saveTrackLog = false,
				)
			}
			return MangaUpdates(
				manga = details,
				newChapters = emptyList(),
			)
		}
		return when {

			// the same chapters count
			chapters.size == track.knownChaptersCount -> {
				if (chapters.lastOrNull()?.id == track.lastChapterId) {
					// manga was not updated. skip
					MangaUpdates(
						manga = details,
						newChapters = emptyList(),
					)
				} else {
					// number of chapters still the same, bu last chapter changed.
					// maybe some chapters are removed. we need to find last known chapter
					val knownChapter = chapters.indexOfLast { it.id == track.lastChapterId }
					if (knownChapter == -1) {
						// confuse. reset anything
						if (commit) {
							repository.storeTrackResult(
								mangaId = track.manga.id,
								knownChaptersCount = chapters.size,
								lastChapterId = chapters.lastOrNull()?.id ?: 0L,
								previousTrackChapterId = 0L,
								newChapters = emptyList(),
								saveTrackLog = false,
							)
						}
						MangaUpdates(
							manga = details,
							newChapters = emptyList(),
						)
					} else {
						val newChapters = chapters.takeLast(chapters.size - knownChapter + 1)
						if (commit) {
							repository.storeTrackResult(
								mangaId = track.manga.id,
								knownChaptersCount = knownChapter + 1,
								lastChapterId = track.lastChapterId,
								previousTrackChapterId = track.lastNotifiedChapterId,
								newChapters = newChapters,
								saveTrackLog = true,
							)
						}
						MangaUpdates(
							manga = details,
							newChapters = details.getNewChapters(track.lastNotifiedChapterId),
						)
					}
				}
			}
			else -> {
				val newChapters = chapters.takeLast(chapters.size - track.knownChaptersCount)
				if (commit) {
					repository.storeTrackResult(
						mangaId = track.manga.id,
						knownChaptersCount = track.knownChaptersCount,
						lastChapterId = track.lastChapterId,
						previousTrackChapterId = track.lastNotifiedChapterId,
						newChapters = newChapters,
						saveTrackLog = true,
					)
				}
				MangaUpdates(
					manga = details,
					newChapters = details.getNewChapters(track.lastNotifiedChapterId),
				)
			}
		}
	}

	private fun Manga.getNewChapters(lastChapterId: Long): List<MangaChapter> {
		val chapters = chapters ?: return emptyList()
		if (lastChapterId == 0L) {
			return emptyList()
		}
		val raw = chapters.takeLastWhile { x -> x.id != lastChapterId }
		return if (raw.isEmpty() || raw.size == chapters.size) {
			emptyList()
		} else {
			raw
		}
	}
}