package org.koitharu.kotatsu.suggestions.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toEntities
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.util.ext.mapItems
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.suggestions.data.SuggestionEntity
import javax.inject.Inject

class SuggestionRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	fun observeAll(): Flow<List<Manga>> {
		return db.suggestionDao.observeAll().mapItems {
			it.manga.toManga(it.tags.toMangaTags())
		}
	}

	fun observeAll(limit: Int): Flow<List<Manga>> {
		return db.suggestionDao.observeAll(limit).mapItems {
			it.manga.toManga(it.tags.toMangaTags())
		}
	}

	suspend fun getRandom(): Manga? {
		return db.suggestionDao.getRandom()?.let {
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
			suggestions.forEach { (manga, relevance) ->
				val tags = manga.tags.toEntities()
				db.tagsDao.upsert(tags)
				db.mangaDao.upsert(manga.toEntity(), tags)
				db.suggestionDao.upsert(
					SuggestionEntity(
						mangaId = manga.id,
						relevance = relevance,
						createdAt = System.currentTimeMillis(),
					),
				)
			}
		}
	}
}
