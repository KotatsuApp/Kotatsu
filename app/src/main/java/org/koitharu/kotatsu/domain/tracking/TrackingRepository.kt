package org.koitharu.kotatsu.domain.tracking

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.TrackEntity
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaTracking
import java.util.*

class TrackingRepository : KoinComponent {

	private val db: MangaDatabase by inject()

	suspend fun getNewChaptersCount(mangaId: Long): Int {
		val entity = db.tracksDao.find(mangaId) ?: return 0
		return entity.newChapters
	}

	suspend fun getAllTracks(): List<MangaTracking> {
		val favourites = db.favouritesDao.findAllManga()
		val history = db.historyDao.findAllManga()
		val manga = (favourites + history).distinctBy { it.id }
		val tracks = db.tracksDao.findAll().groupBy { it.mangaId }
		return manga.map { m ->
			val track = tracks[m.id]?.singleOrNull()
			MangaTracking(
				manga = m.toManga(),
				knownChaptersCount = track?.totalChapters ?: -1,
				lastChapterId = track?.lastChapterId ?: 0L,
				lastNotifiedChapterId = track?.lastNotifiedChapterId ?: 0L,
				lastCheck = track?.lastCheck?.takeUnless { it == 0L }?.let(::Date)
			)
		}
	}

	suspend fun storeTrackResult(
		mangaId: Long,
		knownChaptersCount: Int,
		lastChapterId: Long,
		newChapters: Int,
		lastNotifiedChapterId: Long
	) {
		val entity = TrackEntity(
			mangaId = mangaId,
			newChapters = newChapters,
			lastCheck = System.currentTimeMillis(),
			lastChapterId = lastChapterId,
			totalChapters = knownChaptersCount,
			lastNotifiedChapterId = lastNotifiedChapterId
		)
		db.tracksDao.upsert(entity)
	}

	suspend fun insertOrNothing(manga: Manga) {
		val chapters = manga.chapters ?: return
		val entity = TrackEntity(
			mangaId = manga.id,
			totalChapters = chapters.size,
			lastChapterId = chapters.lastOrNull()?.id ?: 0L,
			newChapters = 0,
			lastCheck = System.currentTimeMillis(),
			lastNotifiedChapterId = 0L
		)
		db.tracksDao.insert(entity)
	}
}