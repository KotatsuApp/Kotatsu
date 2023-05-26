package org.koitharu.kotatsu.core.network.cookies

import android.webkit.CookieManager
import androidx.annotation.WorkerThread
import okhttp3.Cookie
import okhttp3.HttpUrl
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidCookieJar : MutableCookieJar {

	private val cookieManager = CookieManager.getInstance()

	@WorkerThread
	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		val rawCookie = cookieManager.getCookie(url.toString()) ?: return emptyList()
		return rawCookie.split(';').mapNotNull {
			Cookie.parse(url, it)
		}
	}

	@WorkerThread
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		if (cookies.isEmpty()) {
			return
		}
		val urlString = url.toString()
		for (cookie in cookies) {
			cookieManager.setCookie(urlString, cookie.toString())
		}
	}

	override suspend fun clear() = suspendCoroutine<Boolean> { continuation ->
		cookieManager.removeAllCookies(continuation::resume)
	}
}
