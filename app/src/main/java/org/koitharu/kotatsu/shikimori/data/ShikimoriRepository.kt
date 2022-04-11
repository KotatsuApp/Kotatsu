package org.koitharu.kotatsu.shikimori.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.shikimori.data.model.ShikimoriUser

private const val CLIENT_ID = "Mw6F0tPEOgyV7F9U9Twg50Q8SndMY7hzIOfXg0AX_XU"
private const val CLIENT_SECRET = "euBMt1GGRSDpVIFQVPxZrO7Kh6X4gWyv0dABuj4B-M8"
private const val REDIRECT_URI = "kotatsu://shikimori-auth"

class ShikimoriRepository(
	private val okHttp: OkHttpClient,
	private val storage: ShikimoriStorage,
) {

	val oauthUrl: String
		get() = "https://shikimori.one/oauth/authorize?client_id=$CLIENT_ID&" +
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
			.url("https://shikimori.one/oauth/token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getString("refresh_token")
	}

	suspend fun getUser(): ShikimoriUser {
		val request = Request.Builder()
			.get()
			.url("https://shikimori.one/api/users/whoami")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return ShikimoriUser(response)
	}
}