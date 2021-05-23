package org.koitharu.kotatsu.base.domain

import okhttp3.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.utils.ext.await

open class MangaLoaderContext(
	private val okHttp: OkHttpClient,
	private val cookieJar: CookieJar
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
		form: Map<String, String>
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
		payload: String
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

	open fun getSettings(source: MangaSource) = SourceSettings(get(), source)

	fun insertCookies(domain: String, vararg cookies: String) {
		val url = HttpUrl.Builder()
			.scheme(SCHEME_HTTP)
			.host(domain)
			.build()
		cookieJar.saveFromResponse(url, cookies.mapNotNull {
			Cookie.parse(url, it)
		})
	}

	private companion object {

		private const val SCHEME_HTTP = "http"
	}
}