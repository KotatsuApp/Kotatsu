package org.koitharu.kotatsu.domain

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

	suspend fun savePreferences(mangaId: Long, mode: ReaderMode) {
		db.preferencesDao().upsert(
			MangaPrefsEntity(
				mangaId = mangaId,
				mode = mode.id
			)
		)
	}

	suspend fun getReaderMode(mangaId: Long): ReaderMode? {
		return db.preferencesDao().find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun findMangaById(mangaId: Long): Manga? {
		return db.mangaDao().find(mangaId)?.toManga()
	}

	suspend fun storeManga(manga: Manga) {
		db.mangaDao().upsert(MangaEntity.from(manga), manga.tags.map(TagEntity.Companion::fromMangaTag))
	}
}