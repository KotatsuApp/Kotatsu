package org.koitharu.kotatsu.scrobbling.mal.data

import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.network.CommonHeaders
import java.io.IOException

class MALInterceptor(private val storage: MALStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val sourceRequest = chain.request()
		val request = sourceRequest.newBuilder()
		if (!sourceRequest.url.pathSegments.contains("oauth2")) {
			storage.accessToken?.let {
				request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
			}
		}
		val response = chain.proceed(request.build())
		if (!response.isSuccessful && !response.isRedirect) {
			throw IOException("${response.code} ${response.message}")
		}
		return response
	}

}
