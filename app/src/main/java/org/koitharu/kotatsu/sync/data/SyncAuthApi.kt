package org.koitharu.kotatsu.sync.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.utils.ext.toRequestBody

class SyncAuthApi(
	context: Context,
	private val okHttpClient: OkHttpClient,
) {

	private val baseUrl = context.getString(R.string.url_sync_server)

	suspend fun authenticate(email: String, password: String): String {
		val body = JSONObject(
			mapOf("email" to email, "password" to password)
		).toRequestBody()
		val request = Request.Builder()
			.url("$baseUrl/auth")
			.post(body)
			.build()
		val response = okHttpClient.newCall(request).await().parseJson()
		return response.getString("token")
	}
}