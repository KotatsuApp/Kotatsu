package org.koitharu.kotatsu.scrobbling.mal.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.scrobbling.data.ScrobblerRepository
import org.koitharu.kotatsu.scrobbling.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerUser
import org.koitharu.kotatsu.utils.PKCEGenerator

private const val REDIRECT_URI = "kotatsu://mal-auth"
private const val BASE_OAUTH_URL = "https://myanimelist.net"
private const val BASE_API_URL = "https://api.myanimelist.net/v2"
private const val MANGA_PAGE_SIZE = 250

// af16954886b040673378423f5d62cccd

class MALRepository(
	private val okHttp: OkHttpClient,
	private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	private var codeVerifier: String = ""

	override val oauthUrl: String
		get() = "${BASE_OAUTH_URL}/v1/oauth2/authorize?" +
			"response_type=code" +
			"&client_id=af16954886b040673378423f5d62cccd" +
			"&redirect_uri=${REDIRECT_URI}" +
			"&code_challenge=${getPKCEChallengeCode()}" +
			"&code_challenge_method=plain"

	override val isAuthorized: Boolean
		get() = storage.accessToken != null
	override val cachedUser: ScrobblerUser?
		get() = TODO("Not yet implemented")

	override suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		if (code != null) {
			body.add("client_id", "af16954886b040673378423f5d62cccd")
			body.add("code", code)
			body.add("code_verifier", getPKCEChallengeCode())
			body.add("grant_type", "authorization_code")
		}
		val request = Request.Builder()
			.post(body.build())
			.url("${BASE_OAUTH_URL}/v1/oauth2/token")

		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getString("refresh_token")
	}

	override suspend fun loadUser(): ScrobblerUser {
		val request = Request.Builder()
			.get()
			.url("${BASE_API_URL}/users")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return MALUser(response).also { storage.user = it }
	}

	override suspend fun unregister(mangaId: Long) {
		return db.scrobblingDao.delete(ScrobblerService.MAL.id, mangaId)
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

	override fun logout() {
		storage.clear()
	}

	private fun getPKCEChallengeCode(): String {
		codeVerifier = PKCEGenerator.generateCodeVerifier()
		return codeVerifier
	}

	private fun MALUser(json: JSONObject) = ScrobblerUser(
		id = json.getLong("id"),
		nickname = json.getString("nickname"),
		avatar = json.getString("avatar"),
		service = ScrobblerService.SHIKIMORI,
	)

}
