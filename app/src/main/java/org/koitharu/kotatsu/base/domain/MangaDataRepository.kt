package org.koitharu.kotatsu.base.domain

import androidx.room.withTransaction
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.ReaderMode

class MangaDataRepository(private val db: MangaDatabase) {

	suspend fun savePreferences(manga: Manga, mode: ReaderMode) {
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(MangaEntity.from(manga), tags)
			db.preferencesDao.upsert(
				MangaPrefsEntity(
					mangaId = manga.id,
					mode = mode.id
				)
			)
		}
	}

	suspend fun getReaderMode(mangaId: Long): ReaderMode? {
		return db.preferencesDao.find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun findMangaById(mangaId: Long): Manga? {
		return db.mangaDao.find(mangaId)?.toManga()
	}

	suspend fun resolveIntent(intent: MangaIntent): Manga? = when {
		intent.manga != null -> intent.manga
		intent.mangaId != MangaIntent.ID_NONE -> db.mangaDao.find(intent.mangaId)?.toManga()
		else -> null // TODO resolve uri
	}

	suspend fun storeManga(manga: Manga) {
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(MangaEntity.from(manga), tags)
		}
	}
}