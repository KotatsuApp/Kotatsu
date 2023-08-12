package org.koitharu.kotatsu.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.core.exceptions.TooManyRequestExceptions
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RateLimitInterceptor : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		if (response.code == 429) {
			val retryDate = response.header(CommonHeaders.RETRY_AFTER)?.parseRetryDate()
			val request = response.request
			response.closeQuietly()
			throw TooManyRequestExceptions(
				url = request.url.toString(),
				retryAt = retryDate,
			)
		}
		return response
	}

	private fun String.parseRetryDate(): Instant? {
		return toLongOrNull()?.let { Instant.now().plusSeconds(it) }
			?: ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
	}
}
