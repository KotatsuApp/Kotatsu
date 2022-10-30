package org.koitharu.kotatsu.scrobbling.anilist.data

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.exception.GraphQLException
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.scrobbling.data.ScrobblerRepository
import org.koitharu.kotatsu.scrobbling.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.data.ScrobblingEntity
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerUser
import org.koitharu.kotatsu.utils.ext.toRequestBody

private const val REDIRECT_URI = "kotatsu://anilist-auth"
private const val BASE_URL = "https://anilist.co/api/v2/"
private const val ENDPOINT = "https://graphql.anilist.co"
private const val MANGA_PAGE_SIZE = 10

class AniListRepository(
	private val okHttp: OkHttpClient,
	private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	override val oauthUrl: String
		get() = "${BASE_URL}oauth/authorize?client_id=${BuildConfig.ANILIST_CLIENT_ID}&" +
			"redirect_uri=${REDIRECT_URI}&response_type=code"

	override val isAuthorized: Boolean
		get() = storage.accessToken != null

	override suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		body.add("client_id", BuildConfig.ANILIST_CLIENT_ID)
		body.add("client_secret", BuildConfig.ANILIST_CLIENT_SECRET)
		if (code != null) {
			body.add("grant_type", "authorization_code")
			body.add("redirect_uri", REDIRECT_URI)
			body.add("code", code)
		} else {
			body.add("grant_type", "refresh_token")
			body.add("refresh_token", checkNotNull(storage.refreshToken))
		}
		val request = Request.Builder()
			.post(body.build())
			.url("${BASE_URL}oauth/token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getString("refresh_token")
	}

	override suspend fun loadUser(): ScrobblerUser {
		val response = query(
			"""
			AniChartUser {
				user {
					id
					name
					avatar {
						medium
					}
				}
			}
		""".trimIndent(),
		)
		val jo = response.getJSONObject("data").getJSONObject("AniChartUser").getJSONObject("user")
		return AniListUser(jo).also { storage.user = it }
	}

	override val cachedUser: ScrobblerUser?
		get() {
			return storage.user
		}

	override suspend fun unregister(mangaId: Long) {
		return db.scrobblingDao.delete(ScrobblerService.SHIKIMORI.id, mangaId)
	}

	override fun logout() {
		storage.clear()
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		val page = offset / MANGA_PAGE_SIZE
		val response = query(
			"""
			Page(page: $page, perPage: ${MANGA_PAGE_SIZE}) {
				media(type: MANGA, isAdult: true, sort: SEARCH_MATCH, search: "${JSONObject.quote(query)}") {
					id
					title {
						userPreferred
						native
					}
					coverImage {
						medium
					}
					siteUrl
				}
			}
		""".trimIndent(),
		)
		val data = response.getJSONObject("data").getJSONObject("Page").getJSONArray("media")
		return data.mapJSON { ScrobblerManga(it) }
	}

	override suspend fun createRate(mangaId: Long, scrobblerMangaId: Long) {
		val response = query(
			"""
			mutation {
				SaveMediaListEntry(mediaId: $scrobblerMangaId) {
					id
					mediaId
					status
					notes
					scoreRaw
					progress
				}
			}
			""".trimIndent(),
		)
		saveRate(response, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, chapter: MangaChapter) {
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

	override suspend fun updateRate(rateId: Int, mangaId: Long, rating: Float, status: String?, comment: String?) {
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

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		val response = query(
			"""
			Media(id: $id) {
				id
				title {
					userPreferred
				}
				coverImage {
					large
				}
				description
				siteUrl
			}
			""".trimIndent(),
		)
		return ScrobblerMangaInfo(response.getJSONObject("data").getJSONObject("Media"))
	}

	private suspend fun saveRate(json: JSONObject, mangaId: Long) {
		val entity = ScrobblingEntity(
			scrobbler = ScrobblerService.SHIKIMORI.id,
			id = json.getInt("id"),
			mangaId = mangaId,
			targetId = json.getLong("mediaId"),
			status = json.getString("status"),
			chapter = json.getInt("progress"),
			comment = json.getString("notes"),
			rating = json.getDouble("scoreRaw").toFloat() / 100f,
		)
		db.scrobblingDao.insert(entity)
	}

	private fun ScrobblerManga(json: JSONObject): ScrobblerManga {
		val title = json.getJSONObject("title")
		return ScrobblerManga(
			id = json.getLong("id"),
			name = title.getString("userPreferred"),
			altName = title.getStringOrNull("native"),
			cover = json.getJSONObject("coverImage").getString("medium"),
			url = json.getString("siteUrl"),
		)
	}

	private fun ScrobblerMangaInfo(json: JSONObject) = ScrobblerMangaInfo(
		id = json.getLong("id"),
		name = json.getJSONObject("title").getString("userPreferred"),
		cover = json.getJSONObject("coverImage").getString("large"),
		url = json.getString("siteUrl"),
		descriptionHtml = json.getString("description"),
	)

	private fun AniListUser(json: JSONObject) = ScrobblerUser(
		id = json.getLong("id"),
		nickname = json.getString("name"),
		avatar = json.getJSONObject("avatar").getString("medium"),
		service = ScrobblerService.ANILIST,
	)

	private suspend fun query(query: String): JSONObject {
		val body = JSONObject()
		body.put("query", "{$query}")
		body.put("variables", null)
		val mediaType = "application/json; charset=utf-8".toMediaType()
		val requestBody = body.toString().toRequestBody(mediaType)
		val request = Request.Builder()
			.post(requestBody)
			.url(ENDPOINT)
		val json = okHttp.newCall(request.build()).await().parseJson()
		json.optJSONArray("errors")?.let {
			if (it.length() != 0) {
				throw GraphQLException(it)
			}
		}
		return json
	}
}
