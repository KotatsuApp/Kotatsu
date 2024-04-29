package org.koitharu.kotatsu.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.jsoup.Jsoup
import org.koitharu.kotatsu.core.exceptions.CloudFlareBlockedException
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_UNAVAILABLE

class CloudFlareInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		if (response.code == HTTP_FORBIDDEN || response.code == HTTP_UNAVAILABLE) {
			val content = response.body?.let { response.peekBody(Long.MAX_VALUE) }?.byteStream()?.use {
				Jsoup.parse(it, Charsets.UTF_8.name(), response.request.url.toString())
			} ?: return response
			val hasCaptcha = content.getElementById("challenge-error-title") != null
			val isBlocked = content.selectFirst("h2[data-translate=\"blocked_why_headline\"]") != null
			if (hasCaptcha || isBlocked) {
				val request = response.request
				response.closeQuietly()
				if (isBlocked) {
					throw CloudFlareBlockedException(
						url = request.url.toString(),
						source = request.tag(MangaSource::class.java),
					)
				} else {
					throw CloudFlareProtectedException(
						url = request.url.toString(),
						source = request.tag(MangaSource::class.java),
						headers = request.headers,
					)
				}
			}
		}
		return response
	}
}
