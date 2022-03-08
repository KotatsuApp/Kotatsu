package org.koitharu.kotatsu.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_UNAVAILABLE

private const val HEADER_SERVER = "Server"
private const val SERVER_CLOUDFLARE = "cloudflare"

class CloudFlareInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		if (response.code == HTTP_FORBIDDEN || response.code == HTTP_UNAVAILABLE) {
			if (response.header(HEADER_SERVER)?.startsWith(SERVER_CLOUDFLARE) == true) {
				response.closeQuietly()
				throw CloudFlareProtectedException(chain.request().url.toString())
			}
		}
		return response
	}
}