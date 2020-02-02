package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.HistoryEntity
import org.koitharu.kotatsu.core.db.entity.HistoryWithManga
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.model.*
import java.io.Closeable

class HistoryRepository() : KoinComponent, MangaRepository, Closeable {

	private val db: MangaDatabase by inject()

	override val sortOrders: Set<SortOrder> = setOf(SortOrder.NEWEST, SortOrder.POPULARITY)

	override val isSearchAvailable = false

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tags: Set<String>?
	): List<Manga> = getHistory(offset, query, sortOrder, tags).map { x -> x.manga }

	suspend fun getHistory(
		offset: Int,
		query: String? = null,
		sortOrder: SortOrder? = null,
		tags: Set<String>? = null
	): List<MangaInfo<MangaHistory>> {
		val entities = db.historyDao().getAll(offset, 20, "updated_by")
		return entities.map { x -> MangaInfo(x.manga.toManga(), x.history.toMangaHistory()) }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		throw UnsupportedOperationException("History repository does not support getDetails() method")
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		throw UnsupportedOperationException("History repository does not support getPages() method")
	}

	override suspend fun getPageFullUrl(page: MangaPage) = page.url

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

	suspend fun clear() {
		db.historyDao().clear()
	}

	override fun close() {
		db.close()
	}
}