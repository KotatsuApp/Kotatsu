package org.koitharu.kotatsu.core.util.ext

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import okio.IOException
import org.json.JSONObject
import java.net.HttpURLConnection

private val TYPE_JSON = "application/json".toMediaType()

fun JSONObject.toRequestBody() = toString().toRequestBody(TYPE_JSON)

fun Response.parseJsonOrNull(): JSONObject? {
	return try {
		when {
			!isSuccessful -> throw IOException(body?.string())
			code == HttpURLConnection.HTTP_NO_CONTENT -> null
			else -> JSONObject(body?.string() ?: return null)
		}
	} finally {
		closeQuietly()
	}
}
