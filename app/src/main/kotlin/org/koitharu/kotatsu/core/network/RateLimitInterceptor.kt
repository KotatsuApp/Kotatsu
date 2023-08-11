package org.koitharu.kotatsu.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.core.exceptions.TooManyRequestExceptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RateLimitInterceptor : Interceptor {

	private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZ", Locale.ENGLISH)

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

	private fun String.parseRetryDate(): Date? {
		toIntOrNull()?.let {
			return Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(it.toLong()))
		}
		return dateFormat.parse(this)
	}
}
