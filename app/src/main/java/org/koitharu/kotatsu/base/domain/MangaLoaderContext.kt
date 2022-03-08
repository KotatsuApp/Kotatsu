package org.koitharu.kotatsu.base.domain

import android.annotation.SuppressLint
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.core.exceptions.GraphQLException
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.parseJson
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class MangaLoaderContext(
	private val okHttp: OkHttpClient,
	val cookieJar: CookieJar,
) : KoinComponent {

	suspend fun httpGet(url: String, headers: Headers? = null): Response {
		val request = Request.Builder()
			.get()
			.url(url)
		if (headers != null) {
			request.headers(headers)
		}
		return okHttp.newCall(request.build()).await()
	}

	suspend fun httpPost(
		url: String,
		form: Map<String, String>,
	): Response {
		val body = FormBody.Builder()
		form.forEach { (k, v) ->
			body.addEncoded(k, v)
		}
		val request = Request.Builder()
			.post(body.build())
			.url(url)
		return okHttp.newCall(request.build()).await()
	}

	suspend fun httpPost(
		url: String,
		payload: String,
	): Response {
		val body = FormBody.Builder()
		payload.split('&').forEach {
			val pos = it.indexOf('=')
			if (pos != -1) {
				val k = it.substring(0, pos)
				val v = it.substring(pos + 1)
				body.addEncoded(k, v)
			}
		}
		val request = Request.Builder()
			.post(body.build())
			.url(url)
		return okHttp.newCall(request.build()).await()
	}

	suspend fun graphQLQuery(endpoint: String, query: String): JSONObject {
		val body = JSONObject()
		body.put("operationName", null)
		body.put("variables", JSONObject())
		body.put("query", "{${query}}")
		val mediaType = "application/json; charset=utf-8".toMediaType()
		val requestBody = body.toString().toRequestBody(mediaType)
		val request = Request.Builder()
			.post(requestBody)
			.url(endpoint)
		val json = okHttp.newCall(request.build()).await().parseJson()
		json.optJSONArray("errors")?.let {
			if (it.length() != 0) {
				throw GraphQLException(it)
			}
		}
		return json
	}

	@SuppressLint("SetJavaScriptEnabled")
	open suspend fun evaluateJs(script: String): String? = withContext(Dispatchers.Main) {
		val webView = WebView(get())
		webView.settings.javaScriptEnabled = true
		suspendCoroutine { cont ->
			webView.evaluateJavascript(script) { result ->
				cont.resume(result?.takeUnless { it == "null" })
			}
		}
	}

	open fun getSettings(source: MangaSource) = SourceSettings(get(), source)
}