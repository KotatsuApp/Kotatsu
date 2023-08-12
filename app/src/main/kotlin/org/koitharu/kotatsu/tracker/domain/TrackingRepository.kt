package org.koitharu.kotatsu.tracker.domain

import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.util.ext.mapItems
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.tracker.data.TrackEntity
import org.koitharu.kotatsu.tracker.data.TrackLogEntity
import org.koitharu.kotatsu.tracker.data.toTrackingLogItem
import org.koitharu.kotatsu.tracker.domain.model.MangaTracking
import org.koitharu.kotatsu.tracker.domain.model.MangaUpdates
import org.koitharu.kotatsu.tracker.domain.model.TrackingLogItem
import java.util.Date
import javax.inject.Inject

private const val NO_ID = 0L

@Reusable
class TrackingRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	private var isGcCalled = false

	suspend fun getNewChaptersCount(mangaId: Long): Int {
		return db.tracksDao.findNewChapters(mangaId) ?: 0
	}

	fun observeNewChaptersCount(mangaId: Long): Flow<Int> {
		return db.tracksDao.observeNewChapters(mangaId).map { it ?: 0 }
	}

	fun observeUpdatedMangaCount(): Flow<Int> {
		return db.tracksDao.observeNewChapters().map { list -> list.count { it > 0 } }
			.onStart { gcIfNotCalled() }
	}

	fun observeUpdatedManga(limit: Int = 0): Flow<List<Manga>> {
		return if (limit == 0) {
			db.tracksDao.observeUpdatedManga()
		} else {
			db.tracksDao.observeUpdatedManga(limit)
		}.mapItems { it.toManga() }
			.distinctUntilChanged()
			.onStart { gcIfNotCalled() }
	}

	suspend fun getTracks(mangaList: Collection<Manga>): List<MangaTracking> {
		val ids = mangaList.mapToSet { it.id }
		val tracks = db.tracksDao.findAll(ids).groupBy { it.mangaId }
		val idSet = HashSet<Long>()
		val result = ArrayList<MangaTracking>(mangaList.size)
		for (item in mangaList) {
			if (item.source == MangaSource.LOCAL || !idSet.add(item.id)) {
				continue
			}
			val track = tracks[item.id]?.lastOrNull()
			result += MangaTracking(
				manga = item,
				lastChapterId = track?.lastChapterId ?: NO_ID,
				lastCheck = track?.lastCheck?.takeUnless { it == 0L }?.let(::Date),
			)
		}
		return result
	}

	@VisibleForTesting
	suspend fun getTrack(manga: Manga): MangaTracking {
		val track = db.tracksDao.find(manga.id)
		return MangaTracking(
			manga = manga,
			lastChapterId = track?.lastChapterId ?: NO_ID,
			lastCheck = track?.lastCheck?.takeUnless { it == 0L }?.let(::Date),
		)
	}

	@VisibleForTesting
	suspend fun deleteTrack(mangaId: Long) {
		db.tracksDao.delete(mangaId)
	}

	fun observeTrackingLog(limit: Flow<Int>): Flow<List<TrackingLogItem>> {
		return limit.flatMapLatest { limitValue ->
			combine(
				db.tracksDao.observeNewChaptersMap(),
				db.trackLogsDao.observeAll(limitValue),
			) { counters, entities ->
				val countersMap = counters.toMutableMap()
				entities.map { x -> x.toTrackingLogItem(countersMap) }
			}
		}.onStart {
			gcIfNotCalled()
		}
	}

	suspend fun getLogsCount() = db.trackLogsDao.count()

	suspend fun clearLogs() = db.trackLogsDao.clear()

	suspend fun clearCounters() = db.tracksDao.clearCounters()

	suspend fun gc() {
		db.tracksDao.gc()
		db.trackLogsDao.gc()
	}

	suspend fun saveUpdates(updates: MangaUpdates.Success) {
		db.withTransaction {
			val track = getOrCreateTrack(updates.manga.id).mergeWith(updates)
			db.tracksDao.upsert(track)
			if (updates.isValid && updates.newChapters.isNotEmpty()) {
				updatePercent(updates)
				val logEntity = TrackLogEntity(
					mangaId = updates.manga.id,
					chapters = updates.newChapters.joinToString("\n") { x -> x.name },
					createdAt = System.currentTimeMillis(),
				)
				db.trackLogsDao.insert(logEntity)
			}
		}
	}

	suspend fun clearUpdates(ids: Collection<Long>) {
		when {
			ids.isEmpty() -> return
			ids.size == 1 -> db.tracksDao.clearCounter(ids.single())
			else -> db.withTransaction {
				for (id in ids) {
					db.tracksDao.clearCounter(id)
				}
			}
		}
	}

	suspend fun syncWithHistory(manga: Manga, chapterId: Long) {
		val chapters = manga.chapters ?: return
		val chapterIndex = chapters.indexOfFirst { x -> x.id == chapterId }
		val track = getOrCreateTrack(manga.id)
		val lastNewChapterIndex = chapters.size - track.newChapters
		val lastChapterId = chapters.lastOrNull()?.id ?: NO_ID
		val entity = TrackEntity(
			mangaId = manga.id,
			totalChapters = chapters.size,
			lastChapterId = lastChapterId,
			newChapters = when {
				track.newChapters == 0 -> 0
				chapterIndex < 0 -> track.newChapters
				chapterIndex >= lastNewChapterIndex -> chapters.lastIndex - chapterIndex
				else -> track.newChapters
			},
			lastCheck = System.currentTimeMillis(),
			lastNotifiedChapterId = lastChapterId,
		)
		db.tracksDao.upsert(entity)
	}

	suspend fun getCategoriesCount(): IntArray {
		val categories = db.favouriteCategoriesDao.findAll()
		return intArrayOf(
			categories.count { it.track },
			categories.size,
		)
	}

	suspend fun getAllFavouritesManga(): Map<FavouriteCategory, List<Manga>> {
		val categories = db.favouriteCategoriesDao.findAll()
		return categories.associateTo(LinkedHashMap(categories.size)) { categoryEntity ->
			categoryEntity.toFavouriteCategory() to
				db.favouritesDao.findAllManga(categoryEntity.categoryId).toMangaList()
		}
	}

	suspend fun getAllHistoryManga(): List<Manga> {
		return db.historyDao.findAllManga().toMangaList()
	}

	private suspend fun getOrCreateTrack(mangaId: Long): TrackEntity {
		return db.tracksDao.find(mangaId) ?: TrackEntity(
			mangaId = mangaId,
			totalChapters = 0,
			lastChapterId = 0L,
			newChapters = 0,
			lastCheck = 0L,
			lastNotifiedChapterId = 0L,
		)
	}

	private suspend fun updatePercent(updates: MangaUpdates.Success) {
		val history = db.historyDao.find(updates.manga.id) ?: return
		val chapters = updates.manga.chapters
		if (chapters.isNullOrEmpty()) {
			return
		}
		val chapterIndex = chapters.indexOfFirst { it.id == history.chapterId }
		if (chapterIndex < 0) {
			return
		}
		val position = (chapters.size - updates.newChapters.size) * history.percent
		val newPercent = position / chapters.size.toFloat()
		db.historyDao.update(history.copy(percent = newPercent))
	}

	private fun TrackEntity.mergeWith(updates: MangaUpdates.Success): TrackEntity {
		val chapters = updates.manga.chapters.orEmpty()
		return TrackEntity(
			mangaId = mangaId,
			totalChapters = chapters.size,
			lastChapterId = chapters.lastOrNull()?.id ?: NO_ID,
			newChapters = if (updates.isValid) newChapters + updates.newChapters.size else 0,
			lastCheck = System.currentTimeMillis(),
			lastNotifiedChapterId = NO_ID,
		)
	}

	private suspend fun gcIfNotCalled() {
		if (!isGcCalled) {
			gc()
			isGcCalled = true
		}
	}

	private fun Collection<MangaEntity>.toMangaList() = map { it.toManga(emptySet()) }
}
