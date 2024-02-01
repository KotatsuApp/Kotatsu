package org.koitharu.kotatsu.scrobbling.mal.data

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.parsers.util.mimeType
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerStorage

private const val JSON = "application/json"
private const val HTML = "text/html"

class MALInterceptor(private val storage: ScrobblerStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val sourceRequest = chain.request()
		val request = sourceRequest.newBuilder()
		request.header(CommonHeaders.CONTENT_TYPE, JSON)
		request.header(CommonHeaders.ACCEPT, JSON)
		if (!sourceRequest.url.pathSegments.contains("oauth")) {
			storage.accessToken?.let {
				request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
			}
		}
		val response = chain.proceed(request.build())
		if (response.mimeType == HTML) {
			throw IOException(response.parseHtml().title())
		}
		return response
	}

}
