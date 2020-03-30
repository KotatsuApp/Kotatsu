package org.koitharu.kotatsu.domain

import androidx.room.withTransaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.ReaderMode

class MangaDataRepository : KoinComponent {

	private val db: MangaDatabase by inject()

	suspend fun savePreferences(manga: Manga, mode: ReaderMode) {
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.withTransaction {
			db.tagsDao().upsert(tags)
			db.mangaDao().upsert(MangaEntity.from(manga), tags)
			db.preferencesDao().upsert(
				MangaPrefsEntity(
					mangaId = manga.id,
					mode = mode.id
				)
			)
		}
	}

	suspend fun getReaderMode(mangaId: Long): ReaderMode? {
		return db.preferencesDao().find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun findMangaById(mangaId: Long): Manga? {
		return db.mangaDao().find(mangaId)?.toManga()
	}

	suspend fun storeManga(manga: Manga) {
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.withTransaction {
			db.tagsDao().upsert(tags)
			db.mangaDao().upsert(MangaEntity.from(manga), tags)
		}
	}
}