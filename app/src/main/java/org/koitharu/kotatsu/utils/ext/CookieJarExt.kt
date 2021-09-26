package org.koitharu.kotatsu.utils.ext

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

private const val SCHEME_HTTPS = "https"

fun CookieJar.insertCookies(domain: String, vararg cookies: String) {
	val url = HttpUrl.Builder()
		.scheme(SCHEME_HTTPS)
		.host(domain)
		.build()
	saveFromResponse(url, cookies.mapNotNull {
		Cookie.parse(url, it)
	})
}

fun CookieJar.getCookies(domain: String): List<Cookie> {
	val url = HttpUrl.Builder()
		.scheme(SCHEME_HTTPS)
		.host(domain)
		.build()
	return loadForRequest(url)
}

fun CookieJar.copyCookies(oldDomain: String, newDomain: String, names: Array<String>? = null) {
	val url = HttpUrl.Builder()
		.scheme(SCHEME_HTTPS)
		.host(oldDomain)
	var cookies = loadForRequest(url.build())
	if (names != null) {
		cookies = cookies.filter { c -> c.name in names }
	}
	url.host(newDomain)
	saveFromResponse(url.build(), cookies)
}