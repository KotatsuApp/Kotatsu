package org.koitharu.kotatsu.scrobbling.shikimori.data

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.parseJsonArray
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.scrobbling.data.ScrobblingEntity
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.shikimori.data.model.ShikimoriUser
import org.koitharu.kotatsu.utils.ext.toRequestBody

private const val REDIRECT_URI = "kotatsu://shikimori-auth"
private const val BASE_URL = "https://shikimori.one/"
private const val MANGA_PAGE_SIZE = 10

class ShikimoriRepository(
	private val okHttp: OkHttpClient,
	private val storage: ShikimoriStorage,
	private val db: MangaDatabase,
) {

	val oauthUrl: String
		get() = "${BASE_URL}oauth/authorize?client_id=${BuildConfig.SHIKIMORI_CLIENT_ID}&" +
			"redirect_uri=$REDIRECT_URI&response_type=code&scope="

	val isAuthorized: Boolean
		get() = storage.accessToken != null

	suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		body.add("grant_type", "authorization_code")
		body.add("client_id", BuildConfig.SHIKIMORI_CLIENT_ID)
		body.add("client_secret", BuildConfig.SHIKIMORI_CLIENT_SECRET)
		if (code != null) {
			body.add("redirect_uri", REDIRECT_URI)
			body.add("code", code)
		} else {
			body.add("refresh_token", checkNotNull(storage.refreshToken))
		}
		val request = Request.Builder()
			.post(body.build())
			.url("${BASE_URL}oauth/token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getString("refresh_token")
	}

	suspend fun loadUser(): ShikimoriUser {
		val request = Request.Builder()
			.get()
			.url("${BASE_URL}api/users/whoami")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return ShikimoriUser(response).also { storage.user = it }
	}

	fun getCachedUser(): ShikimoriUser? {
		return storage.user
	}

	suspend fun unregister(mangaId: Long) {
		return db.scrobblingDao.delete(ScrobblerService.SHIKIMORI.id, mangaId)
	}

	fun logout() {
		storage.clear()
	}

	suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		val page = offset / MANGA_PAGE_SIZE
		val pageOffset = offset % MANGA_PAGE_SIZE
		val url = BASE_URL.toHttpUrl().newBuilder()
			.addPathSegment("api")
			.addPathSegment("mangas")
			.addEncodedQueryParameter("page", (page + 1).toString())
			.addEncodedQueryParameter("limit", MANGA_PAGE_SIZE.toString())
			.addEncodedQueryParameter("censored", false.toString())
			.addQueryParameter("search", query)
			.build()
		val request = Request.Builder().url(url).get().build()
		val response = okHttp.newCall(request).await().parseJsonArray()
		val list = response.mapJSON { ScrobblerManga(it) }
		return if (pageOffset != 0) list.drop(pageOffset) else list
	}

	suspend fun createRate(mangaId: Long, shikiMangaId: Long) {
		val user = getCachedUser() ?: loadUser()
		val payload = JSONObject()
		payload.put(
			"user_rate",
			JSONObject().apply {
				put("target_id", shikiMangaId)
				put("target_type", "Manga")
				put("user_id", user.id)
			},
		)
		val url = BASE_URL.toHttpUrl().newBuilder()
			.addPathSegment("api")
			.addPathSegment("v2")
			.addPathSegment("user_rates")
			.build()
		val request = Request.Builder().url(url).post(payload.toRequestBody()).build()
		val response = okHttp.newCall(request).await().parseJson()
		saveRate(response, mangaId)
	}

	suspend fun updateRate(rateId: Int, mangaId: Long, chapter: MangaChapter) {
		val payload = JSONObject()
		payload.put(
			"user_rate",
			JSONObject().apply {
				put("chapters", chapter.number)
			},
		)
		val url = BASE_URL.toHttpUrl().newBuilder()
			.addPathSegment("api")
			.addPathSegment("v2")
			.addPathSegment("user_rates")
			.addPathSegment(rateId.toString())
			.build()
		val request = Request.Builder().url(url).patch(payload.toRequestBody()).build()
		val response = okHttp.newCall(request).await().parseJson()
		saveRate(response, mangaId)
	}

	suspend fun updateRate(rateId: Int, mangaId: Long, rating: Float, status: String?, comment: String?) {
		val payload = JSONObject()
		payload.put(
			"user_rate",
			JSONObject().apply {
				put("score", rating.toString())
				if (comment != null) {
					put("text", comment)
				}
				if (status != null) {
					put("status", status)
				}
			},
		)
		val url = BASE_URL.toHttpUrl().newBuilder()
			.addPathSegment("api")
			.addPathSegment("v2")
			.addPathSegment("user_rates")
			.addPathSegment(rateId.toString())
			.build()
		val request = Request.Builder().url(url).patch(payload.toRequestBody()).build()
		val response = okHttp.newCall(request).await().parseJson()
		saveRate(response, mangaId)
	}

	suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		val request = Request.Builder()
			.get()
			.url("${BASE_URL}api/mangas/$id")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return ScrobblerMangaInfo(response)
	}

	private suspend fun saveRate(json: JSONObject, mangaId: Long) {
		val entity = ScrobblingEntity(
			scrobbler = ScrobblerService.SHIKIMORI.id,
			id = json.getInt("id"),
			mangaId = mangaId,
			targetId = json.getLong("target_id"),
			status = json.getString("status"),
			chapter = json.getInt("chapters"),
			comment = json.getString("text"),
			rating = json.getDouble("score").toFloat() / 10f,
		)
		db.scrobblingDao.insert(entity)
	}

	private fun ScrobblerManga(json: JSONObject) = ScrobblerManga(
		id = json.getLong("id"),
		name = json.getString("name"),
		altName = json.getStringOrNull("russian"),
		cover = json.getJSONObject("image").getString("preview").toAbsoluteUrl("shikimori.one"),
		url = json.getString("url").toAbsoluteUrl("shikimori.one"),
	)

	private fun ScrobblerMangaInfo(json: JSONObject) = ScrobblerMangaInfo(
		id = json.getLong("id"),
		name = json.getString("name"),
		cover = json.getJSONObject("image").getString("preview").toAbsoluteUrl("shikimori.one"),
		url = json.getString("url").toAbsoluteUrl("shikimori.one"),
		descriptionHtml = json.getString("description_html"),
	)
}
