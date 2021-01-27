package org.koitharu.kotatsu.parsers

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class TemporaryCookieJar : CookieJar {

	private val cache = HashMap<CookieKey, Cookie>()

	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		val time = System.currentTimeMillis()
		return cache.values.filter { it.matches(url) && it.expiresAt < time }
	}

	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		cookies.forEach {
			val key = CookieKey(url.host, it.name)
			cache[key] = it
		}
	}

	private data class CookieKey(
		val host: String,
		val name: String
	)
}