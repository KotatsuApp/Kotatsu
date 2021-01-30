package org.koitharu.kotatsu.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.HttpURLConnection
import java.net.URI

object AssertX : KoinComponent {

	private val okHttp by inject<OkHttpClient>()

	fun assertContentType(message: String, url: String, vararg types: String) {
		Assert.assertFalse("URL is empty: $message", url.isEmpty())
		val request = Request.Builder()
			.url(url)
			.head()
			.build()
		val response = okHttp.newCall(request).execute()
		when (val code = response.code) {
			HttpURLConnection.HTTP_OK -> {
				val type = response.body!!.contentType()
				Assert.assertTrue(types.any {
					val x = it.split('/')
					type?.type == x[0] && (x[1] == "*" || type.subtype == x[1])
				})
			}
			else -> Assert.fail("Invalid response code $code at $url: $message")
		}
	}

	fun assertUrlRelative(message: String, url: String) {
		Assert.assertFalse(message, URI(url).isAbsolute)
	}

	fun assertUrlAbsolute(message: String, url: String) {
		Assert.assertTrue(message, URI(url).isAbsolute)
	}

}