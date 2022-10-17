package org.koitharu.kotatsu.scrobbling.mal.domain

import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.scrobbling.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.scrobbling.mal.data.MALRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val RATING_MAX = 10f

@Singleton
class MALScrobbler @Inject constructor(
	private val repository: MALRepository,
	db: MangaDatabase,
) : Scrobbler(db, ScrobblerService.MAL) {

	init {
		statuses[ScrobblingStatus.PLANNED] = "plan_to_read"
		statuses[ScrobblingStatus.READING] = "reading"
		statuses[ScrobblingStatus.COMPLETED] = "completed"
		statuses[ScrobblingStatus.ON_HOLD] = "on_hold"
		statuses[ScrobblingStatus.DROPPED] = "dropped"
	}

	override val isAvailable: Boolean
		get() = repository.isAuthorized

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		TODO()
	}

	override suspend fun linkManga(mangaId: Long, targetId: Long) {
		TODO()
	}

	override suspend fun scrobble(mangaId: Long, chapter: MangaChapter) {
		TODO()
	}

	override suspend fun updateScrobblingInfo(
		mangaId: Long,
		rating: Float,
		status: ScrobblingStatus?,
		comment: String?,
	) {
		TODO()
	}

	override suspend fun unregisterScrobbling(mangaId: Long) {
		repository.unregister(mangaId)
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		TODO()
	}
}
