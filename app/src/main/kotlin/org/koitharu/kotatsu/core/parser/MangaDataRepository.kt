package org.koitharu.kotatsu.core.parser

import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.db.entity.toEntities
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter
import javax.inject.Inject

@Reusable
class MangaDataRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	suspend fun saveReaderMode(manga: Manga, mode: ReaderMode) {
		db.withTransaction {
			storeManga(manga)
			val entity = db.preferencesDao.find(manga.id) ?: newEntity(manga.id)
			db.preferencesDao.upsert(entity.copy(mode = mode.id))
		}
	}

	suspend fun saveColorFilter(manga: Manga, colorFilter: ReaderColorFilter?) {
		db.withTransaction {
			storeManga(manga)
			val entity = db.preferencesDao.find(manga.id) ?: newEntity(manga.id)
			db.preferencesDao.upsert(
				entity.copy(
					cfBrightness = colorFilter?.brightness ?: 0f,
					cfContrast = colorFilter?.contrast ?: 0f,
					cfInvert = colorFilter?.isInverted ?: false,
				),
			)
		}
	}

	suspend fun getReaderMode(mangaId: Long): ReaderMode? {
		return db.preferencesDao.find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun getColorFilter(mangaId: Long): ReaderColorFilter? {
		return db.preferencesDao.find(mangaId)?.getColorFilterOrNull()
	}

	fun observeColorFilter(mangaId: Long): Flow<ReaderColorFilter?> {
		return db.preferencesDao.observe(mangaId)
			.map { it?.getColorFilterOrNull() }
			.distinctUntilChanged()
	}

	suspend fun findMangaById(mangaId: Long): Manga? {
		return db.mangaDao.find(mangaId)?.toManga()
	}

	suspend fun resolveIntent(intent: MangaIntent): Manga? = when {
		intent.manga != null -> intent.manga
		intent.mangaId != 0L -> findMangaById(intent.mangaId)
		else -> null // TODO resolve uri
	}

	suspend fun storeManga(manga: Manga) {
		val tags = manga.tags.toEntities()
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(manga.toEntity(), tags)
		}
	}

	suspend fun findTags(source: MangaSource): Set<MangaTag> {
		return db.tagsDao.findTags(source.name).toMangaTags()
	}

	private fun MangaPrefsEntity.getColorFilterOrNull(): ReaderColorFilter? {
		return if (cfBrightness != 0f || cfContrast != 0f || cfInvert) {
			ReaderColorFilter(cfBrightness, cfContrast, cfInvert)
		} else {
			null
		}
	}

	private fun newEntity(mangaId: Long) = MangaPrefsEntity(
		mangaId = mangaId,
		mode = -1,
		cfBrightness = 0f,
		cfContrast = 0f,
		cfInvert = false,
	)
}
