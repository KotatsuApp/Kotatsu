package org.koitharu.kotatsu.scrobbling.kitsu.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.util.ext.parseJsonOrNull
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.json.getFloatOrDefault
import org.koitharu.kotatsu.parsers.util.json.getIntOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.urlEncoded
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerRepository
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblingEntity
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser
import org.koitharu.kotatsu.scrobbling.kitsu.data.KitsuInterceptor.Companion.VND_JSON

private const val BASE_WEB_URL = "https://kitsu.app"

class KitsuRepository(
	@ApplicationContext context: Context,
	private val okHttp: OkHttpClient,
	private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	// not in use yet
	private val clientId = context.getString(R.string.kitsu_clientId)
	private val clientSecret = context.getString(R.string.kitsu_clientSecret)

	override val oauthUrl: String = "kotatsu+kitsu://auth"

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
			body.add("username", code.substringBefore(';'))
			body.add("password", code.substringAfter(';'))
		} else {
			body.add("grant_type", "refresh_token")
			body.add("refresh_token", checkNotNull(storage.refreshToken))
		}
		val request = Request.Builder()
			.post(body.build())
			.url("$BASE_WEB_URL/api/oauth/token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getString("refresh_token")
	}

	override suspend fun loadUser(): ScrobblerUser {
		val request = Request.Builder()
			.get()
			.url("$BASE_WEB_URL/api/edge/users?filter[self]=true")
		val response = okHttp.newCall(request.build()).await().parseJson()
			.getJSONArray("data")
			.getJSONObject(0)
		return ScrobblerUser(
			id = response.getAsLong("id"),
			nickname = response.getJSONObject("attributes").getString("name"),
			avatar = response.getJSONObject("attributes").optJSONObject("avatar")?.getStringOrNull("small"),
			service = ScrobblerService.KITSU,
		).also { storage.user = it }
	}

	override fun logout() {
		storage.clear()
	}

	override suspend fun unregister(mangaId: Long) {
		return db.getScrobblingDao().delete(ScrobblerService.KITSU.id, mangaId)
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		val request = Request.Builder()
			.get()
			.url("$BASE_WEB_URL/api/edge/manga?page[limit]=20&page[offset]=$offset&filter[text]=${query.urlEncoded()}")
		val response = okHttp.newCall(request.build()).await().parseJson().ensureSuccess()
		return response.getJSONArray("data").mapJSON { jo ->
			val attrs = jo.getJSONObject("attributes")
			val titles = attrs.getJSONObject("titles").valuesToStringList()
			ScrobblerManga(
				id = jo.getAsLong("id"),
				name = titles.first(),
				altName = titles.drop(1).joinToString(),
				cover = attrs.getJSONObject("posterImage").getStringOrNull("small").orEmpty(),
				url = "$BASE_WEB_URL/manga/${attrs.getString("slug")}",
			)
		}
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		val request = Request.Builder()
			.get()
			.url("$BASE_WEB_URL/api/edge/manga/$id")
		val data = okHttp.newCall(request.build()).await().parseJson().ensureSuccess().getJSONObject("data")
		val attrs = data.getJSONObject("attributes")
		return ScrobblerMangaInfo(
			id = data.getAsLong("id"),
			name = attrs.getString("canonicalTitle"),
			cover = attrs.getJSONObject("posterImage").getString("medium"),
			url = "$BASE_WEB_URL/manga/${attrs.getString("slug")}",
			descriptionHtml = attrs.getString("description").replace("\\n", "<br>"),
		)
	}

	override suspend fun createRate(mangaId: Long, scrobblerMangaId: Long) {
		findExistingRate(scrobblerMangaId)?.let {
			saveRate(it, mangaId)
			return
		}
		val user = cachedUser ?: loadUser()
		val payload = JSONObject()
		payload.putJO("data") {
			put("type", "libraryEntries")
			putJO("attributes") {
				put("status", "planned") // will be updated by next call
				put("progress", 0)
			}
			putJO("relationships") {
				putJO("manga") {
					putJO("data") {
						put("type", "manga")
						put("id", scrobblerMangaId)
					}
				}
				putJO("user") {
					putJO("data") {
						put("type", "users")
						put("id", user.id)
					}
				}
			}
		}
		val request = Request.Builder()
			.url("$BASE_WEB_URL/api/edge/library-entries?include=manga")
			.post(payload.toKitsuRequestBody())
		val response = okHttp.newCall(request.build()).await().parseJson().ensureSuccess().getJSONObject("data")
		saveRate(response, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, chapter: Int) {
		val payload = JSONObject()
		payload.putJO("data") {
			put("type", "libraryEntries")
			put("id", rateId)
			putJO("attributes") {
				put("progress", chapter)
			}
		}
		val request = Request.Builder()
			.url("$BASE_WEB_URL/api/edge/library-entries/$rateId?include=manga")
			.patch(payload.toKitsuRequestBody())
		val response = okHttp.newCall(request.build()).await().parseJson().ensureSuccess().getJSONObject("data")
		saveRate(response, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, rating: Float, status: String?, comment: String?) {
		val payload = JSONObject()
		payload.putJO("data") {
			put("type", "libraryEntries")
			put("id", rateId)
			putJO("attributes") {
				put("status", status)
				put("ratingTwenty", (rating * 20).toInt().coerceIn(2, 20))
				put("notes", comment)
			}
		}
		val request = Request.Builder()
			.url("$BASE_WEB_URL/api/edge/library-entries/$rateId?include=manga")
			.patch(payload.toKitsuRequestBody())
		val response = okHttp.newCall(request.build()).await().parseJson().ensureSuccess().getJSONObject("data")
		saveRate(response, mangaId)
	}

	private fun JSONObject.valuesToStringList(): List<String> {
		val result = ArrayList<String>(length())
		for (key in keys()) {
			result.add(getStringOrNull(key) ?: continue)
		}
		return result
	}

	private inline fun JSONObject.putJO(name: String, init: JSONObject.() -> Unit) {
		put(name, JSONObject().apply(init))
	}

	private fun JSONObject.toKitsuRequestBody() = toString().toRequestBody(VND_JSON.toMediaType())

	private suspend fun findExistingRate(scrobblerMangaId: Long): JSONObject? {
		val userId = (cachedUser ?: loadUser()).id
		val request = Request.Builder()
			.get()
			.url("$BASE_WEB_URL/api/edge/library-entries?filter[manga_id]=$scrobblerMangaId&filter[userId]=$userId&include=manga")
		val data = okHttp.newCall(request.build()).await().parseJsonOrNull()?.optJSONArray("data") ?: return null
		return data.optJSONObject(0)
	}

	private suspend fun saveRate(json: JSONObject, mangaId: Long) {
		val attrs = json.getJSONObject("attributes")
		val manga = json.getJSONObject("relationships").getJSONObject("manga").getJSONObject("data")
		val entity = ScrobblingEntity(
			scrobbler = ScrobblerService.KITSU.id,
			id = json.getInt("id"),
			mangaId = mangaId,
			targetId = manga.getAsLong("id"),
			status = attrs.getString("status"),
			chapter = attrs.getIntOrDefault("progress", 0),
			comment = attrs.getStringOrNull("notes"),
			rating = (attrs.getFloatOrDefault("ratingTwenty", 0f) / 20f).coerceIn(0f, 1f),
		)
		db.getScrobblingDao().upsert(entity)
	}

	private fun JSONObject.ensureSuccess(): JSONObject {
		val error = optJSONArray("errors")?.optJSONObject(0) ?: return this
		val title = error.getString("title")
		val detail = error.getStringOrNull("detail")
		throw IOException("$title: $detail")
	}

	private fun JSONObject.getAsLong(name: String): Long = when (val rawValue = opt(name)) {
		is Long -> rawValue
		is Number -> rawValue.toLong()
		is String -> rawValue.toLong()
		else -> throw IllegalArgumentException("Value $rawValue at \"$name\" is not of type long")
	}
}
