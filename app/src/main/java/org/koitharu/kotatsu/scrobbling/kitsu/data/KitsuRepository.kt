package org.koitharu.kotatsu.scrobbling.kitsu.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerRepository
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser

private const val BASE_WEB_URL = "https://kitsu.io"

class KitsuRepository(
	@ApplicationContext context: Context,
	private val okHttp: OkHttpClient,
	private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	private val clientId = context.getString(R.string.kitsu_clientId)
	private val clientSecret = context.getString(R.string.kitsu_clientSecret)

	override val oauthUrl: String = ""

	override val isAuthorized: Boolean
		get() = storage.accessToken != null

	override val cachedUser: ScrobblerUser?
		get() {
			return storage.user
		}

	override suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		if (code != null) {
			body.add("grant_type", "password")
			body.add("username", "test@test")
			body.add("password", "test")
		} else {
			body.add("grant_type", "refresh_token")
			body.add("refresh_token", checkNotNull(storage.refreshToken))
		}
		val request = Request.Builder()
			.post(body.build())
			.url("${BASE_WEB_URL}/api/oauth/token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getString("refresh_token")
	}

	override suspend fun loadUser(): ScrobblerUser {
		TODO("Not yet implemented")
	}

	override fun logout() {
		TODO("Not yet implemented")
	}

	override suspend fun unregister(mangaId: Long) {
		return db.scrobblingDao.delete(ScrobblerService.KITSU.id, mangaId)
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		TODO("Not yet implemented")
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		TODO("Not yet implemented")
	}

	override suspend fun createRate(mangaId: Long, scrobblerMangaId: Long) {
		TODO("Not yet implemented")
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, chapter: MangaChapter) {
		TODO("Not yet implemented")
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, rating: Float, status: String?, comment: String?) {
		TODO("Not yet implemented")
	}

}
