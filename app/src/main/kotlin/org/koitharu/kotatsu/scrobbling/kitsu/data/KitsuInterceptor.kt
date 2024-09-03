package org.koitharu.kotatsu.scrobbling.kitsu.data

import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.common.domain.ScrobblerAuthRequiredException
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import java.net.HttpURLConnection

class KitsuInterceptor(private val storage: ScrobblerStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val sourceRequest = chain.request()
		val request = sourceRequest.newBuilder()
		request.header(CommonHeaders.CONTENT_TYPE, VND_JSON)
		request.header(CommonHeaders.ACCEPT, VND_JSON)
		val isAuthRequest = sourceRequest.url.pathSegments.contains("oauth")
		if (!isAuthRequest) {
			storage.accessToken?.let {
				request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
			}
		}
		val response = chain.proceed(request.build())
		if (!isAuthRequest && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw ScrobblerAuthRequiredException(ScrobblerService.KITSU)
		}
		return response
	}

	companion object {

		const val VND_JSON = "application/vnd.api+json"
	}
}
