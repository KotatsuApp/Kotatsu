package org.koitharu.kotatsu.domain

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.prefs.ReaderMode

class MangaPreferencesRepository : KoinComponent {

	private val db: MangaDatabase by inject()

	suspend fun saveData(mangaId: Long, mode: ReaderMode) {
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
}