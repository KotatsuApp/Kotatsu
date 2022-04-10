package org.koitharu.kotatsu.history.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.mapItems

class HistoryRepository(
	private val db: MangaDatabase,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
) {

	suspend fun getList(offset: Int, limit: Int = 20): List<Manga> {
		val entities = db.historyDao.findAll(offset, limit)
		return entities.map { it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)) }
	}

	suspend fun getLastOrNull(): Manga? {
		val entity = db.historyDao.findAll(0, 1).firstOrNull() ?: return null
		return entity.manga.toManga(entity.tags.mapToSet { it.toMangaTag() })
	}

	fun observeAll(): Flow<List<Manga>> {
		return db.historyDao.observeAll().mapItems {
			it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag))
		}
	}

	fun observeAllWithHistory(): Flow<List<MangaWithHistory>> {
		return db.historyDao.observeAll().mapItems {
			MangaWithHistory(
				it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)),
				it.history.toMangaHistory()
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

	suspend fun addOrUpdate(manga: Manga, chapterId: Long, page: Int, scroll: Int) {
		if (manga.isNsfw && settings.isHistoryExcludeNsfw) {
			return
		}
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(MangaEntity.from(manga), tags)
			db.historyDao.upsert(
				HistoryEntity(
					mangaId = manga.id,
					createdAt = System.currentTimeMillis(),
					updatedAt = System.currentTimeMillis(),
					chapterId = chapterId,
					page = page,
					scroll = scroll.toFloat() // we migrate to int, but decide to not update database
				)
			)
			trackingRepository.upsert(manga)
		}
	}

	suspend fun getOne(manga: Manga): MangaHistory? {
		return db.historyDao.find(manga.id)?.toMangaHistory()
	}

	suspend fun clear() {
		db.historyDao.clear()
	}

	suspend fun delete(manga: Manga) {
		db.historyDao.delete(manga.id)
	}

	suspend fun delete(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.historyDao.delete(id)
			}
		}
	}

	/**
	 * Try to replace one manga with another one
	 * Useful for replacing saved manga on deleting it with remove source
	 */
	suspend fun deleteOrSwap(manga: Manga, alternative: Manga?) {
		if (alternative == null || db.mangaDao.update(MangaEntity.from(alternative)) <= 0) {
			db.historyDao.delete(manga.id)
		}
	}

	suspend fun getPopularTags(limit: Int): List<MangaTag> {
		return db.historyDao.findPopularTags(limit).map { x -> x.toMangaTag() }
	}
}