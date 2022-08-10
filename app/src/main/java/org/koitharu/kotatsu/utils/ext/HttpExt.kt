package org.koitharu.kotatsu.utils.ext

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.util.parseJson
import java.net.HttpURLConnection

private val TYPE_JSON = "application/json".toMediaType()

fun JSONObject.toRequestBody() = toString().toRequestBody(TYPE_JSON)

fun Response.parseJsonOrNull(): JSONObject? {
	return if (code == HttpURLConnection.HTTP_NO_CONTENT) {
		null
	} else {
		parseJson()
	}
}
