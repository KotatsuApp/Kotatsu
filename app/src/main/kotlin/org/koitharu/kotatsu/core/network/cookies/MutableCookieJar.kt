package org.koitharu.kotatsu.core.network.cookies

import androidx.annotation.WorkerThread
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

interface MutableCookieJar : CookieJar {

	@WorkerThread
	override fun loadForRequest(url: HttpUrl): List<Cookie>

	@WorkerThread
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>)

	suspend fun clear(): Boolean
}
