package org.koitharu.kotatsu.core.model

import okhttp3.Headers
import okhttp3.Request

data class RequestDraft(
	val url: String,
	val headers: Headers
) {

	val isValid: Boolean
		get() = url.isNotEmpty()

	fun newBuilder(): Request.Builder = Request.Builder()
		.url(url)
		.get()
		.headers(headers)
}