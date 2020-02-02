package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.HistoryEntity
import org.koitharu.kotatsu.core.db.entity.HistoryWithManga
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import java.util.*

class HistoryRepository : KoinComponent {

	private val db: MangaDatabase by inject()

	suspend fun getList(offset: Int) : List<Manga> {
		val entities = db.historyDao().getAll(offset, 20, "updated_by")
		return entities.map { it.manga.toManga() }
	}

	suspend fun addOrUpdate(manga: Manga, chapterId: Long, page: Int) {
		val dao = db.historyDao()
		val entity = HistoryEntity(
			mangaId = manga.id,
			createdAt = System.currentTimeMillis(),
			updatedAt = System.currentTimeMillis(),
			chapterId = chapterId,
			page = page
		)
		dao.upsert(
			HistoryWithManga(
				history = entity,
				manga = MangaEntity.from(manga)
			)
		)
	}

	suspend fun getOne(manga: Manga): MangaHistory? {
		return db.historyDao().getOneOrNull(manga.id)?.let {
			MangaHistory(
				createdAt = Date(it.createdAt),
				updatedAt = Date(it.updatedAt),
				chapterId = it.chapterId,
				page = it.page
			)
		}
	}

	suspend fun clear() {
		db.historyDao().clear()
	}
}