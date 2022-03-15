package org.koitharu.kotatsu.tracker.domain

import androidx.room.withTransaction
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.TrackEntity
import org.koitharu.kotatsu.core.db.entity.TrackLogEntity
import org.koitharu.kotatsu.core.model.MangaTracking
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.util.*

class TrackingRepository(
	private val db: MangaDatabase,
) {

	suspend fun getNewChaptersCount(mangaId: Long): Int {
		return db.tracksDao.findNewChapters(mangaId) ?: 0
	}

	suspend fun getAllTracks(useFavourites: Boolean, useHistory: Boolean): List<MangaTracking> {
		val mangaList = ArrayList<Manga>()
		if (useFavourites) {
			db.favouritesDao.findAllManga().mapTo(mangaList) { it.toManga() }
		}
		if (useHistory) {
			db.historyDao.findAllManga().mapTo(mangaList) { it.toManga() }
		}
		val tracks = db.tracksDao.findAll().groupBy { it.mangaId }
		return mangaList
			.filterNot { it.source == MangaSource.LOCAL }
			.distinctBy { it.id }
			.map { manga ->
				val track = tracks[manga.id]?.singleOrNull()
				MangaTracking(
					manga = manga,
					knownChaptersCount = track?.totalChapters ?: -1,
					lastChapterId = track?.lastChapterId ?: 0L,
					lastNotifiedChapterId = track?.lastNotifiedChapterId ?: 0L,
					lastCheck = track?.lastCheck?.takeUnless { it == 0L }?.let(::Date)
				)
			}
	}

	suspend fun getTrackingLog(offset: Int, limit: Int): List<TrackingLogItem> {
		return db.trackLogsDao.findAll(offset, limit).map { x ->
			x.toTrackingLogItem()
		}
	}

	suspend fun count() = db.trackLogsDao.count()

	suspend fun clearLogs() = db.trackLogsDao.clear()

	suspend fun cleanup() {
		db.withTransaction {
			db.tracksDao.cleanup()
			db.trackLogsDao.cleanup()
		}
	}

	suspend fun storeTrackResult(
		mangaId: Long,
		knownChaptersCount: Int,
		lastChapterId: Long,
		newChapters: List<MangaChapter>,
		previousTrackChapterId: Long
	) {
		db.withTransaction {
			val entity = TrackEntity(
				mangaId = mangaId,
				newChapters = newChapters.size,
				lastCheck = System.currentTimeMillis(),
				lastChapterId = lastChapterId,
				totalChapters = knownChaptersCount,
				lastNotifiedChapterId = newChapters.lastOrNull()?.id ?: previousTrackChapterId
			)
			db.tracksDao.upsert(entity)
			val foundChapters = newChapters.takeLastWhile { x -> x.id != previousTrackChapterId }
			if (foundChapters.isNotEmpty()) {
				val logEntity = TrackLogEntity(
					mangaId = mangaId,
					chapters = foundChapters.joinToString("\n") { x -> x.name },
					createdAt = System.currentTimeMillis()
				)
				db.trackLogsDao.insert(logEntity)
			}
		}
	}

	suspend fun upsert(manga: Manga) {
		val chapters = manga.chapters ?: return
		val entity = TrackEntity(
			mangaId = manga.id,
			totalChapters = chapters.size,
			lastChapterId = chapters.lastOrNull()?.id ?: 0L,
			newChapters = 0,
			lastCheck = System.currentTimeMillis(),
			lastNotifiedChapterId = 0L
		)
		db.tracksDao.upsert(entity)
	}
}