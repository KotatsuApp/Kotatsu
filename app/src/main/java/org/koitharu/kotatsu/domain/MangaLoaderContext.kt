package org.koitharu.kotatsu.domain

import android.content.Context
import okhttp3.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.utils.ext.await

class MangaLoaderContext : KoinComponent {

	private val okHttp by inject<OkHttpClient>()

	suspend fun get(url: String, block: (Request.Builder.() -> Unit)? = null): Response {
		val request = Request.Builder()
			.get()
			.url(url)
		if (block != null) {
			request.block()
		}
		return okHttp.newCall(request.build()).await()
	}

	suspend fun post(
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
}