package org.koitharu.kotatsu.domain

import okhttp3.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.SourceConfig
import org.koitharu.kotatsu.utils.ext.await

open class MangaLoaderContext : KoinComponent {

	private val okHttp by inject<OkHttpClient>()
	private val cookieJar by inject<CookieJar>()

	suspend fun httpGet(url: String, block: (Request.Builder.() -> Unit)? = null): Response {
		val request = Request.Builder()
			.get()
			.url(url)
		if (block != null) {
			request.block()
		}
		return okHttp.newCall(request.build()).await()
	}

	suspend fun httpPost(
		url: String,
		form: Map<String, String>,
		block: (Request.Builder.() -> Unit)? = null
	): Response {
		val body = FormBody.Builder()
		form.forEach { (k, v) ->
			body.addEncoded(k, v)
		}
		val request = Request.Builder()
			.post(body.build())
			.url(url)
		if (block != null) {
			request.block()
		}
		return okHttp.newCall(request.build()).await()
	}

	open fun getSettings(source: MangaSource) = SourceConfig(get(), source)

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