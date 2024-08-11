package org.koitharu.kotatsu.sync.data

import dagger.Reusable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.core.exceptions.SyncApiException
import org.koitharu.kotatsu.core.network.BaseHttpClient
import org.koitharu.kotatsu.core.util.ext.toRequestBody
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.removeSurrounding
import javax.inject.Inject

@Reusable
class SyncAuthApi @Inject constructor(
	@BaseHttpClient private val okHttpClient: OkHttpClient,
) {

	suspend fun authenticate(syncURL: String, email: String, password: String): String {
		val body = JSONObject(
			mapOf("email" to email, "password" to password),
		).toRequestBody()
		val request = Request.Builder()
			.url("$syncURL/auth")
			.post(body)
			.build()
		val response = okHttpClient.newCall(request).await()
		if (response.isSuccessful) {
			return response.parseJson().getString("token")
		} else {
			val code = response.code
			val message = response.use { checkNotNull(it.body).string() }.removeSurrounding('"')
			throw SyncApiException(message, code)
		}
	}
}
