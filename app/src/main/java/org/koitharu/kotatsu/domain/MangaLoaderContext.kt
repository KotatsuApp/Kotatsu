package org.koitharu.kotatsu.domain

import android.content.Context
import okhttp3.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.utils.ext.await

class MangaLoaderContext(context: Context) : KoinComponent {

	private val okHttp by inject<OkHttpClient>()
	private val preferences = context.getSharedPreferences("sources", Context.MODE_PRIVATE)

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

	fun getStringOption(name: String, default: String? = null) =
		preferences.getString(name, default)

	fun getIntOption(name: String, default: Int) = preferences.getInt(name, default)

	fun getBooleanOption(name: String, default: Boolean) = preferences.getBoolean(name, default)
}