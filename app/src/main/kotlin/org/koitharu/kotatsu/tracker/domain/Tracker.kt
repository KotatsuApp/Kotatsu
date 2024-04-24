package org.koitharu.kotatsu.tracker.domain

import androidx.annotation.VisibleForTesting
import coil.request.CachePolicy
import dagger.Reusable
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.MultiMutex
import org.koitharu.kotatsu.core.util.ext.toInstantOrNull
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.tracker.domain.model.MangaTracking
import org.koitharu.kotatsu.tracker.domain.model.MangaUpdates
import java.time.Instant
import javax.inject.Inject
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Reusable
class Tracker @Inject constructor(
	private val settings: AppSettings,
	private val repository: TrackingRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend fun getTracks(limit: Int): List<MangaTracking> {
		repository.updateTracks()
		return repository.getTracks(offset = 0, limit = limit)
	}

	suspend fun gc() {
		repository.gc()
	}

	suspend fun fetchUpdates(
		track: MangaTracking,
		commit: Boolean
	): MangaUpdates = withMangaLock(track.manga.id) {
		val updates = runCatchingCancellable {
			val repo = mangaRepositoryFactory.create(track.manga.source)
			require(repo is RemoteMangaRepository) { "Repository ${repo.javaClass.simpleName} is not supported" }
			val manga = repo.getDetails(track.manga, CachePolicy.WRITE_ONLY)
			compare(track, manga, getBranch(manga))
		}.getOrElse { error ->
			MangaUpdates.Failure(
				manga = track.manga,
				error = error,
			)
		}
		if (commit) {
			repository.saveUpdates(updates)
		}
		return updates
	}

	suspend fun syncWithDetails(details: Manga) {
		requireNotNull(details.chapters)
		val track = repository.getTrackOrNull(details) ?: return
		val updates = compare(track, details, getBranch(details))
		repository.saveUpdates(updates)
	}

	suspend fun syncWithHistory(details: Manga, chapterId: Long) {
		val chapters = requireNotNull(details.chapters)
		val track = repository.getTrackOrNull(details) ?: return
		val chapterIndex = chapters.indexOfFirst { x -> x.id == chapterId }
		val lastNewChapterIndex = chapters.size - track.newChapters
		val lastChapter = chapters.lastOrNull()
		val tracking = MangaTracking(
			manga = details,
			lastChapterId = lastChapter?.id ?: NO_ID,
			lastCheck = Instant.now(),
			lastChapterDate = lastChapter?.uploadDate?.toInstantOrNull() ?: track.lastChapterDate,
			newChapters = when {
				track.newChapters == 0 -> 0
				chapterIndex < 0 -> track.newChapters
				chapterIndex >= lastNewChapterIndex -> chapters.lastIndex - chapterIndex
				else -> track.newChapters
			},
		)
		repository.mergeWith(tracking)
	}

	@VisibleForTesting
	suspend fun checkUpdates(manga: Manga, commit: Boolean): MangaUpdates.Success {
		val track = repository.getTrack(manga)
		val updates = compare(track, manga, getBranch(manga))
		if (commit) {
			repository.saveUpdates(updates)
		}
		return updates
	}

	@VisibleForTesting
	suspend fun deleteTrack(mangaId: Long) = withMangaLock(mangaId) {
		repository.deleteTrack(mangaId)
	}

	private suspend fun getBranch(manga: Manga): String? {
		val history = historyRepository.getOne(manga)
		return manga.getPreferredBranch(history)
	}

	/**
	 * The main functionality of tracker: check new chapters in [manga] comparing to the [track]
	 */
	private fun compare(track: MangaTracking, manga: Manga, branch: String?): MangaUpdates.Success {
		if (track.isEmpty()) {
			// first check or manga was empty on last check
			return MangaUpdates.Success(manga, emptyList(), isValid = false)
		}
		val chapters = requireNotNull(manga.getChapters(branch))
		val newChapters = chapters.takeLastWhile { x -> x.id != track.lastChapterId }
		return when {
			newChapters.isEmpty() -> {
				MangaUpdates.Success(
					manga = manga,
					newChapters = emptyList(),
					isValid = chapters.lastOrNull()?.id == track.lastChapterId,
				)
			}

			newChapters.size == chapters.size -> {
				MangaUpdates.Success(manga, emptyList(), isValid = false)
			}

			else -> {
				MangaUpdates.Success(manga, newChapters, isValid = true)
			}
		}
	}

	private companion object {

		const val NO_ID = 0L
		private val mangaMutex = MultiMutex<Long>()

		suspend inline fun <T> withMangaLock(id: Long, action: () -> T): T {
			contract {
				callsInPlace(action, InvocationKind.EXACTLY_ONCE)
			}
			mangaMutex.lock(id)
			try {
				return action()
			} finally {
				mangaMutex.unlock(id)
			}
		}
	}
}
