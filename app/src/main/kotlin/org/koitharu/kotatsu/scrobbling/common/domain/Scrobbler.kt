package org.koitharu.kotatsu.scrobbling.common.domain

import androidx.collection.LongSparseArray
import androidx.collection.getOrElse
import androidx.core.text.parseAsHtml
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.util.ext.findKeyByValue
import org.koitharu.kotatsu.core.util.ext.sanitize
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblingEntity
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import java.util.EnumMap

abstract class Scrobbler(
	protected val db: MangaDatabase,
	val scrobblerService: ScrobblerService,
	private val repository: org.koitharu.kotatsu.scrobbling.common.data.ScrobblerRepository,
) {

	private val infoCache = LongSparseArray<ScrobblerMangaInfo>()
	protected val statuses = EnumMap<ScrobblingStatus, String>(ScrobblingStatus::class.java)

	val user: Flow<ScrobblerUser> = flow {
		repository.cachedUser?.let {
			emit(it)
		}
		runCatchingCancellable {
			repository.loadUser()
		}.onSuccess {
			emit(it)
		}.onFailure {
			it.printStackTraceDebug()
		}
	}

	val isAvailable: Boolean
		get() = repository.isAuthorized

	suspend fun authorize(authCode: String): ScrobblerUser {
		repository.authorize(authCode)
		return repository.loadUser()
	}

	fun logout() {
		repository.logout()
	}

	suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		return repository.findManga(query, offset)
	}

	suspend fun linkManga(mangaId: Long, targetId: Long) {
		repository.createRate(mangaId, targetId)
	}

	suspend fun scrobble(mangaId: Long, chapter: MangaChapter) {
		val entity = db.scrobblingDao.find(scrobblerService.id, mangaId) ?: return
		repository.updateRate(entity.id, entity.mangaId, chapter)
	}

	suspend fun getScrobblingInfoOrNull(mangaId: Long): ScrobblingInfo? {
		val entity = db.scrobblingDao.find(scrobblerService.id, mangaId) ?: return null
		return entity.toScrobblingInfo()
	}

	abstract suspend fun updateScrobblingInfo(mangaId: Long, rating: Float, status: ScrobblingStatus?, comment: String?)

	fun observeScrobblingInfo(mangaId: Long): Flow<ScrobblingInfo?> {
		return db.scrobblingDao.observe(scrobblerService.id, mangaId)
			.map { it?.toScrobblingInfo() }
	}

	fun observeAllScrobblingInfo(): Flow<List<ScrobblingInfo>> {
		return db.scrobblingDao.observe(scrobblerService.id)
			.map { entities ->
				coroutineScope {
					entities.map {
						async {
							it.toScrobblingInfo()
						}
					}.awaitAll()
				}.filterNotNull()
			}
	}

	suspend fun unregisterScrobbling(mangaId: Long) {
		repository.unregister(mangaId)
	}

	protected suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		return repository.getMangaInfo(id)
	}

	private suspend fun ScrobblingEntity.toScrobblingInfo(): ScrobblingInfo? {
		val mangaInfo = infoCache.getOrElse(targetId) {
			runCatchingCancellable {
				getMangaInfo(targetId)
			}.onFailure {
				it.printStackTraceDebug()
			}.onSuccess {
				infoCache.put(targetId, it)
			}.getOrNull() ?: return null
		}
		return ScrobblingInfo(
			scrobbler = scrobblerService,
			mangaId = mangaId,
			targetId = targetId,
			status = statuses.findKeyByValue(status),
			chapter = chapter,
			comment = comment,
			rating = rating,
			title = mangaInfo.name,
			coverUrl = mangaInfo.cover,
			description = mangaInfo.descriptionHtml.parseAsHtml().sanitize(),
			externalUrl = mangaInfo.url,
		)
	}
}

suspend fun Scrobbler.tryScrobble(mangaId: Long, chapter: MangaChapter): Boolean {
	return runCatchingCancellable {
		scrobble(mangaId, chapter)
	}.onFailure {
		it.printStackTraceDebug()
	}.isSuccess
}
