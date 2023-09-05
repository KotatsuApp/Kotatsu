package org.koitharu.kotatsu.core.network.cookies

import androidx.annotation.WorkerThread
import androidx.core.util.Predicate
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

interface MutableCookieJar : CookieJar {

	@WorkerThread
	override fun loadForRequest(url: HttpUrl): List<Cookie>

	@WorkerThread
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>)

	@WorkerThread
	fun removeCookies(url: HttpUrl, predicate: Predicate<Cookie>?)

	suspend fun clear(): Boolean
}
