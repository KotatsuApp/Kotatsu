package org.koitharu.kotatsu.core.network

import okhttp3.CacheControl

object CommonHeaders {

	const val REFERER = "Referer"
	const val USER_AGENT = "User-Agent"
	const val ACCEPT = "Accept"
	const val CONTENT_DISPOSITION = "Content-Disposition"
	const val COOKIE = "Cookie"

	val CACHE_CONTROL_DISABLED: CacheControl
		get() = CacheControl.Builder().noStore().build()
}
