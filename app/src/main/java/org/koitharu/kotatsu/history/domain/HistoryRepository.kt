package org.koitharu.kotatsu.history.domain

import androidx.room.withTransaction
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.base.domain.ReversibleHandle
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.*
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.history.data.toMangaHistory
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.scrobbling.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.domain.tryScrobble
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.mapItems

const val PROGRESS_NONE = -1f

class HistoryRepository @Inject constructor(
	private val db: MangaDatabase,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
) {

	suspend fun getList(offset: Int, limit: Int = 20): List<Manga> {
		val entities = db.historyDao.findAll(offset, limit)
		return entities.map { it.manga.toManga(it.tags.toMangaTags()) }
	}

	suspend fun getLastOrNull(): Manga? {
		val entity = db.historyDao.findAll(0, 1).firstOrNull() ?: return null
		return entity.manga.toManga(entity.tags.toMangaTags())
	}

	fun observeAll(): Flow<List<Manga>> {
		return db.historyDao.observeAll().mapItems {
			it.manga.toManga(it.tags.toMangaTags())
		}
	}

	fun observeAllWithHistory(): Flow<List<MangaWithHistory>> {
		return db.historyDao.observeAll().mapItems {
			MangaWithHistory(
				it.manga.toManga(it.tags.toMangaTags()),
				it.history.toMangaHistory(),
			)
		}
	}

	fun observeOne(id: Long): Flow<MangaHistory?> {
		return db.historyDao.observe(id).map {
			it?.toMangaHistory()
		}
	}

	fun observeHasItems(): Flow<Boolean> {
		return db.historyDao.observeCount()
			.map { it > 0 }
			.distinctUntilChanged()
	}

	suspend fun addOrUpdate(manga: Manga, chapterId: Long, page: Int, scroll: Int, percent: Float) {
		if (manga.isNsfw && settings.isHistoryExcludeNsfw || settings.isIncognitoModeEnabled) {
			return
		}
		val tags = manga.tags.toEntities()
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(manga.toEntity(), tags)
			db.historyDao.upsert(
				HistoryEntity(
					mangaId = manga.id,
					createdAt = System.currentTimeMillis(),
					updatedAt = System.currentTimeMillis(),
					chapterId = chapterId,
					page = page,
					scroll = scroll.toFloat(), // we migrate to int, but decide to not update database
					percent = percent,
					deletedAt = 0L,
				),
			)
			trackingRepository.syncWithHistory(manga, chapterId)
			val chapter = manga.chapters?.find { x -> x.id == chapterId }
			if (chapter != null) {
				scrobblers.forEach { it.tryScrobble(manga.id, chapter) }
			}
		}
	}

	suspend fun getOne(manga: Manga): MangaHistory? {
		return db.historyDao.find(manga.id)?.toMangaHistory()
	}

	suspend fun getProgress(mangaId: Long): Float {
		return db.historyDao.findProgress(mangaId) ?: PROGRESS_NONE
	}

	suspend fun clear() {
		db.historyDao.clear()
	}

	suspend fun delete(manga: Manga) {
		db.historyDao.delete(manga.id)
	}

	suspend fun deleteAfter(minDate: Long) {
		db.historyDao.deleteAfter(minDate)
	}

	suspend fun delete(ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.historyDao.delete(id)
			}
		}
		return ReversibleHandle {
			recover(ids)
		}
	}

	/**
	 * Try to replace one manga with another one
	 * Useful for replacing saved manga on deleting it with remove source
	 */
	suspend fun deleteOrSwap(manga: Manga, alternative: Manga?) {
		if (alternative == null || db.mangaDao.update(alternative.toEntity()) <= 0) {
			db.historyDao.delete(manga.id)
		}
	}

	suspend fun getPopularTags(limit: Int): List<MangaTag> {
		return db.historyDao.findPopularTags(limit).map { x -> x.toMangaTag() }
	}

	private suspend fun recover(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.historyDao.recover(id)
			}
		}
	}
}
