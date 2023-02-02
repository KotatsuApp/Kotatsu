package org.koitharu.kotatsu.scrobbling.mal.data

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.scrobbling.data.ScrobblerRepository
import org.koitharu.kotatsu.scrobbling.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.data.ScrobblingEntity
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerUser
import org.koitharu.kotatsu.utils.PKCEGenerator

private const val REDIRECT_URI = "kotatsu://mal-auth"
private const val BASE_OAUTH_URL = "https://myanimelist.net"
private const val BASE_API_URL = "https://api.myanimelist.net/v2"
private const val MANGA_PAGE_SIZE = 10
private const val AVATAR_STUB = "https://cdn.myanimelist.net/images/questionmark_50.gif"

class MALRepository(
	private val okHttp: OkHttpClient,
	private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	private var codeVerifier: String = getPKCEChallengeCode()

	override val oauthUrl: String
		get() = "$BASE_OAUTH_URL/v1/oauth2/authorize?" +
			"response_type=code" +
			"&client_id=af16954886b040673378423f5d62cccd" +
			"&redirect_uri=$REDIRECT_URI" +
			"&code_challenge=$codeVerifier" +
			"&code_challenge_method=plain"

	override val isAuthorized: Boolean
		get() = storage.accessToken != null

	override val cachedUser: ScrobblerUser?
		get() {
			return storage.user
		}

	override suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		if (code != null) {
			body.add("client_id", "af16954886b040673378423f5d62cccd")
			body.add("grant_type", "authorization_code")
			body.add("code", code)
			body.add("redirect_uri", REDIRECT_URI)
			body.add("code_verifier", codeVerifier)
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
			.url("${BASE_API_URL}/users/@me")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return MALUser(response).also { storage.user = it }
	}

	override suspend fun unregister(mangaId: Long) {
		return db.scrobblingDao.delete(ScrobblerService.MAL.id, mangaId)
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		val pageOffset = offset % MANGA_PAGE_SIZE
		val url = BASE_API_URL.toHttpUrl().newBuilder()
			.addPathSegment("manga")
			.addQueryParameter("offset", (pageOffset + 1).toString())
			.addQueryParameter("nsfw", "true")
			.addEncodedQueryParameter("q", query.take(64)) // WARNING! MAL API throws a 400 when the query is over 64 characters
			.build()
		val request = Request.Builder().url(url).get().build()
		val response = okHttp.newCall(request).await().parseJson()
		val data = response.getJSONArray("data")
		val mangas = data.mapJSON { jsonToManga(it) }
		return if (pageOffset != 0) mangas.drop(pageOffset) else mangas // TODO
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		val url = BASE_API_URL.toHttpUrl().newBuilder()
			.addPathSegment("manga")
			.addPathSegment(id.toString())
			.addQueryParameter("fields", "synopsis")
			.build()
		val request = Request.Builder().url(url)
		val response = okHttp.newCall(request.build()).await().parseJson()
		return ScrobblerMangaInfo(response)
	}

	override suspend fun createRate(mangaId: Long, scrobblerMangaId: Long) {
		val body = FormBody.Builder()
			.add("status", "reading")
			.add("score", "0")
		val url = BASE_API_URL.toHttpUrl().newBuilder()
			.addPathSegment("manga")
			.addPathSegment(scrobblerMangaId.toString())
			.addPathSegment("my_list_status")
			.addQueryParameter("fields", "synopsis")
			.build()
		val request = Request.Builder()
			.url(url)
			.put(body.build())
			.build()
		val response = okHttp.newCall(request).await().parseJson()
		saveRate(response, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, chapter: MangaChapter) {
		val body = FormBody.Builder()
			.add("status", "reading")
			.add("score", "0")
		val url = BASE_API_URL.toHttpUrl().newBuilder()
			.addPathSegment("manga")
			.addPathSegment(mangaId.toString())
			.addPathSegment("my_list_status")
			.build()
		val request = Request.Builder()
			.url(url)
			.put(body.build())
			.build()
		val response = okHttp.newCall(request).await().parseJson()
		saveRate(response, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, rating: Float, status: String?, comment: String?) {
		val body = FormBody.Builder()
			.add("status", status!!)
			.add("score", rating.toString())
		val url = BASE_API_URL.toHttpUrl().newBuilder()
			.addPathSegment("manga")
			.addPathSegment(mangaId.toString())
			.addPathSegment("my_list_status")
			.build()
		val request = Request.Builder()
			.url(url)
			.put(body.build())
			.build()
		val response = okHttp.newCall(request).await().parseJson()
		saveRate(response, mangaId)
	}

	private suspend fun saveRate(json: JSONObject, mangaId: Long) {
		val entity = ScrobblingEntity(
			scrobbler = ScrobblerService.MAL.id,
			id = mangaId.toInt(),
			mangaId = mangaId,
			targetId = 2, // TODO
			status = json.getString("status"),
			chapter = json.getInt("num_chapters_read"),
			comment = json.getString("comments"),
			rating = json.getDouble("score").toFloat() / 10f,
		)
		db.scrobblingDao.upsert(entity)
	}

	override fun logout() {
		storage.clear()
	}

	private fun getPKCEChallengeCode(): String {
		codeVerifier = PKCEGenerator.generateCodeVerifier()
		return codeVerifier
	}

	private fun jsonToManga(json: JSONObject): ScrobblerManga {
		for (i in 0 until json.length()) {
			val node = json.getJSONObject("node")
			return ScrobblerManga(
				id = node.getLong("id"),
				name = node.getString("title"),
				altName = null,
				cover = node.getJSONObject("main_picture").getString("large"),
				url = "" // TODO
			)
		}
		return ScrobblerManga( // TODO
			id = 1,
			name = "",
			altName = null,
			cover = "",
			url = ""
		)
	}

	private fun ScrobblerMangaInfo(json: JSONObject) = ScrobblerMangaInfo(
		id = json.getLong("id"),
		name = json.getString("title"),
		cover = json.getJSONObject("main_picture").getString("large"),
		url = "", // TODO
		descriptionHtml = json.getString("synopsis"),
	)

	private fun MALUser(json: JSONObject) = ScrobblerUser(
		id = json.getLong("id"),
		nickname = json.getString("name"),
		avatar = json.getString("picture") ?: AVATAR_STUB,
		service = ScrobblerService.MAL,
	)

}
