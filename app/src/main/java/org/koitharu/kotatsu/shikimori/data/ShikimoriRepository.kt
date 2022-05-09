package org.koitharu.kotatsu.shikimori.data

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.parseJsonArray
import org.koitharu.kotatsu.parsers.util.urlEncoded
import org.koitharu.kotatsu.shikimori.data.model.ShikimoriManga
import org.koitharu.kotatsu.shikimori.data.model.ShikimoriMangaInfo
import org.koitharu.kotatsu.shikimori.data.model.ShikimoriUser
import org.koitharu.kotatsu.utils.ext.toRequestBody

private const val CLIENT_ID = "Mw6F0tPEOgyV7F9U9Twg50Q8SndMY7hzIOfXg0AX_XU"
private const val CLIENT_SECRET = "euBMt1GGRSDpVIFQVPxZrO7Kh6X4gWyv0dABuj4B-M8"
private const val REDIRECT_URI = "kotatsu://shikimori-auth"
private const val BASE_URL = "https://shikimori.one/"
private const val MANGA_PAGE_SIZE = 10

class ShikimoriRepository(
	private val okHttp: OkHttpClient,
	private val storage: ShikimoriStorage,
) {

	val oauthUrl: String
		get() = "${BASE_URL}oauth/authorize?client_id=$CLIENT_ID&" +
			"redirect_uri=$REDIRECT_URI&response_type=code&scope="

	val isAuthorized: Boolean
		get() = storage.accessToken != null

	suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		body.add("grant_type", "authorization_code")
		body.add("client_id", CLIENT_ID)
		body.add("client_secret", CLIENT_SECRET)
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

	suspend fun getUser(): ShikimoriUser {
		val request = Request.Builder()
			.get()
			.url("${BASE_URL}api/users/whoami")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return ShikimoriUser(response).also { storage.user = it }
	}

	fun getCachedUser(): ShikimoriUser? {
		return storage.user
	}

	fun logout() {
		storage.clear()
	}

	suspend fun findManga(query: String, offset: Int): List<ShikimoriManga> {
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
		val list = response.mapJSON { ShikimoriManga(it) }
		return if (pageOffset != 0) list.drop(pageOffset) else list
	}

	suspend fun trackManga(manga: Manga, shikiMangaId: Long) {
		val user = getCachedUser() ?: getUser()
		val payload = JSONObject()
		payload.put(
			"user_rate",
			JSONObject().apply {
				put("target_id", shikiMangaId)
				put("target_type", "Manga")
				put("user_id", user.id)
			}
		)
		val url = BASE_URL.toHttpUrl().newBuilder()
			.addPathSegment("api")
			.addPathSegment("v2")
			.addPathSegment("user_rates")
			.build()
		val request = Request.Builder().url(url).post(payload.toRequestBody()).build()
		val response = okHttp.newCall(request).await().parseJson()
	}

	suspend fun findMangaInfo(manga: Manga): ShikimoriMangaInfo? {
		val q = manga.title.urlEncoded()
		val request = Request.Builder()
			.get()
			.url("${BASE_URL}api/mangas?limit=5&search=$q&censored=false")
		val response = okHttp.newCall(request.build()).await().parseJsonArray()
		val candidates = response.mapJSON { ShikimoriManga(it) }
		val bestCandidate = candidates.filter {
			it.name.equals(manga.title, ignoreCase = true) || it.name.equals(manga.altTitle, ignoreCase = true)
		}.singleOrNull() ?: return null
		return getMangaInfo(bestCandidate.id)
	}

	suspend fun getRelatedManga(id: Long): List<ShikimoriManga> {
		val request = Request.Builder()
			.get()
			.url("${BASE_URL}api/mangas/$id/related")
		val response = okHttp.newCall(request.build()).await().parseJsonArray()
		return response.mapJSON { jo -> ShikimoriManga(jo) }
	}

	suspend fun getSimilarManga(id: Long): List<ShikimoriManga> {
		val request = Request.Builder()
			.get()
			.url("${BASE_URL}api/mangas/$id/similar")
		val response = okHttp.newCall(request.build()).await().parseJsonArray()
		return response.mapJSON { jo -> ShikimoriManga(jo) }
	}

	suspend fun getMangaInfo(id: Long): ShikimoriMangaInfo {
		val request = Request.Builder()
			.get()
			.url("${BASE_URL}api/mangas/$id")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return ShikimoriMangaInfo(response)
	}
}