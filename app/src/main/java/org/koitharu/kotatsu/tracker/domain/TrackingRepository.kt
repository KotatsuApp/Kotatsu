package org.koitharu.kotatsu.tracker.domain

import androidx.room.withTransaction
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.*
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.MangaTracking
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.*

class TrackingRepository(
	private val db: MangaDatabase,
) {

	suspend fun getNewChaptersCount(mangaId: Long): Int {
		return db.tracksDao.findNewChapters(mangaId) ?: 0
	}

	suspend fun getHistoryManga(): List<Manga> {
		return db.historyDao.findAllManga().toMangaList()
	}

	suspend fun getFavouritesManga(): Map<FavouriteCategory, List<Manga>> {
		val categories = db.favouriteCategoriesDao.findAll()
		return categories.associateTo(LinkedHashMap(categories.size)) { categoryEntity ->
			categoryEntity.toFavouriteCategory() to db.favouritesDao.findAllManga(categoryEntity.categoryId).toMangaList()
		}
	}

	suspend fun getCategoriesCount(): IntArray {
		val categories = db.favouriteCategoriesDao.findAll()
		return intArrayOf(
			categories.count { it.track },
			categories.size,
		)
	}

	suspend fun getTracks(mangaList: Collection<Manga>): List<MangaTracking> {
		val ids = mangaList.mapToSet { it.id }
		val tracks = db.tracksDao.findAll(ids).groupBy { it.mangaId }
		return mangaList // TODO optimize
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
		knownChaptersCount: Int, // how many chapters user already seen
		lastChapterId: Long, // in upstream manga
		newChapters: List<MangaChapter>,
		previousTrackChapterId: Long, // from previous check
		saveTrackLog: Boolean,
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
			if (saveTrackLog && previousTrackChapterId != 0L) {
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

	private fun Collection<MangaEntity>.toMangaList() = map { it.toManga(emptySet()) }
}