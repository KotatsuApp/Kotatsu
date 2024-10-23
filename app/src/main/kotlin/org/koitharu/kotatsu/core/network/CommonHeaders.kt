package org.koitharu.kotatsu.core.network

import okhttp3.CacheControl

object CommonHeaders {

	const val REFERER = "Referer"
	const val USER_AGENT = "User-Agent"
	const val ACCEPT = "Accept"
	const val CONTENT_TYPE = "Content-Type"
	const val CONTENT_DISPOSITION = "Content-Disposition"
	const val COOKIE = "Cookie"
	const val CONTENT_ENCODING = "Content-Encoding"
	const val ACCEPT_ENCODING = "Accept-Encoding"
	const val AUTHORIZATION = "Authorization"
	const val CACHE_CONTROL = "Cache-Control"
	const val PROXY_AUTHORIZATION = "Proxy-Authorization"
	const val RETRY_AFTER = "Retry-After"
	const val MANGA_SOURCE = "X-Manga-Source"

	val CACHE_CONTROL_NO_STORE: CacheControl
		get() = CacheControl.Builder().noStore().build()
}
