package org.koitharu.kotatsu.suggestions.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.suggestions.data.SuggestionEntity
import org.koitharu.kotatsu.utils.ext.mapItems
import org.koitharu.kotatsu.utils.ext.mapToSet

class SuggestionRepository(
	private val db: MangaDatabase,
) {

	fun observeAll(): Flow<List<Manga>> {
		return db.suggestionDao.observeAll().mapItems {
			it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag))
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
				db.mangaDao.upsert(MangaEntity.from(x.manga))
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