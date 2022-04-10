package org.koitharu.kotatsu.suggestions.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toEntities
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.suggestions.data.SuggestionEntity
import org.koitharu.kotatsu.utils.ext.mapItems

class SuggestionRepository(
	private val db: MangaDatabase,
) {

	fun observeAll(): Flow<List<Manga>> {
		return db.suggestionDao.observeAll().mapItems {
			it.manga.toManga(it.tags.toMangaTags())
		}
	}

	suspend fun clear() {
		db.suggestionDao.deleteAll()
	}

	suspend fun isEmpty(): Boolean {
		return db.suggestionDao.count() == 0
	}

	suspend fun replace(suggestions: Iterable<MangaSuggestion>) {
		db.withTransaction {
			db.suggestionDao.deleteAll()
			suggestions.forEach { x ->
				val tags = x.manga.tags.toEntities()
				db.tagsDao.upsert(tags)
				db.mangaDao.upsert(x.manga.toEntity(), tags)
				db.suggestionDao.upsert(
					SuggestionEntity(
						mangaId = x.manga.id,
						relevance = x.relevance,
						createdAt = System.currentTimeMillis(),
					)
				)
			}
		}
	}
}