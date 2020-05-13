package org.koitharu.kotatsu.parsers

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.koitharu.kotatsu.core.local.cookies.cache.SetCookieCache

class TemporaryCookieJar : CookieJar {

	private val cache = SetCookieCache()

	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		return cache.toList()
	}

	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		cache.addAll(cookies)
	}
}