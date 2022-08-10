package org.koitharu.kotatsu.scrobbling.shikimori.domain

import javax.inject.Inject
import javax.inject.Singleton
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.scrobbling.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriRepository

private const val RATING_MAX = 10f

@Singleton
class ShikimoriScrobbler @Inject constructor(
	private val repository: ShikimoriRepository,
	db: MangaDatabase,
) : Scrobbler(db, ScrobblerService.SHIKIMORI) {

	init {
		statuses[ScrobblingStatus.PLANNED] = "planned"
		statuses[ScrobblingStatus.READING] = "watching"
		statuses[ScrobblingStatus.RE_READING] = "rewatching"
		statuses[ScrobblingStatus.COMPLETED] = "completed"
		statuses[ScrobblingStatus.ON_HOLD] = "on_hold"
		statuses[ScrobblingStatus.DROPPED] = "dropped"
	}

	override val isAvailable: Boolean
		get() = repository.isAuthorized

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		return repository.findManga(query, offset)
	}

	override suspend fun linkManga(mangaId: Long, targetId: Long) {
		repository.createRate(mangaId, targetId)
	}

	override suspend fun scrobble(mangaId: Long, chapter: MangaChapter) {
		val entity = db.scrobblingDao.find(scrobblerService.id, mangaId) ?: return
		repository.updateRate(entity.id, entity.mangaId, chapter)
	}

	override suspend fun updateScrobblingInfo(
		mangaId: Long,
		rating: Float,
		status: ScrobblingStatus?,
		comment: String?,
	) {
		val entity = db.scrobblingDao.find(scrobblerService.id, mangaId)
		requireNotNull(entity) { "Scrobbling info for manga $mangaId not found" }
		repository.updateRate(
			rateId = entity.id,
			mangaId = entity.mangaId,
			rating = rating * RATING_MAX,
			status = statuses[status],
			comment = comment,
		)
	}

	override suspend fun unregisterScrobbling(mangaId: Long) {
		repository.unregister(mangaId)
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		return repository.getMangaInfo(id)
	}
}
