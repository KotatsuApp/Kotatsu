package org.koitharu.kotatsu.scrobbling.domain

import androidx.collection.LongSparseArray
import androidx.collection.getOrElse
import androidx.core.text.parseAsHtml
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.scrobbling.data.ScrobblingEntity
import org.koitharu.kotatsu.scrobbling.domain.model.*
import org.koitharu.kotatsu.utils.ext.findKey
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

abstract class Scrobbler(
	protected val db: MangaDatabase,
	val scrobblerService: ScrobblerService,
) {

	private val infoCache = LongSparseArray<ScrobblerMangaInfo>()
	protected val statuses = EnumMap<ScrobblingStatus, String>(ScrobblingStatus::class.java)

	abstract val isAvailable: Boolean

	abstract suspend fun findManga(query: String, offset: Int): List<ScrobblerManga>

	abstract suspend fun linkManga(mangaId: Long, targetId: Long)

	abstract suspend fun scrobble(mangaId: Long, chapter: MangaChapter)

	suspend fun getScrobblingInfoOrNull(mangaId: Long): ScrobblingInfo? {
		val entity = db.scrobblingDao.find(scrobblerService.id, mangaId) ?: return null
		return entity.toScrobblingInfo(mangaId)
	}

	abstract suspend fun updateScrobblingInfo(mangaId: Long, rating: Float, status: ScrobblingStatus?, comment: String?)

	fun observeScrobblingInfo(mangaId: Long): Flow<ScrobblingInfo?> {
		return db.scrobblingDao.observe(scrobblerService.id, mangaId)
			.map { it?.toScrobblingInfo(mangaId) }
	}

	protected abstract suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo

	private suspend fun ScrobblingEntity.toScrobblingInfo(mangaId: Long): ScrobblingInfo? {
		val mangaInfo = infoCache.getOrElse(targetId) {
			runCatching {
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
			status = statuses.findKey(status),
			chapter = chapter,
			comment = comment,
			rating = rating,
			title = mangaInfo.name,
			coverUrl = mangaInfo.cover,
			description = mangaInfo.descriptionHtml.parseAsHtml(),
			externalUrl = mangaInfo.url,
		)
	}
}

suspend fun Scrobbler.tryScrobble(mangaId: Long, chapter: MangaChapter): Boolean {
	return runCatching {
		scrobble(mangaId, chapter)
	}.onFailure {
		it.printStackTraceDebug()
	}.isSuccess
}