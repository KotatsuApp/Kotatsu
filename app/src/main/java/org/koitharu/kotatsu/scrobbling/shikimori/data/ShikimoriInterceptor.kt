package org.koitharu.kotatsu.scrobbling.shikimori.data

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import org.koitharu.kotatsu.core.network.CommonHeaders

private const val USER_AGENT_SHIKIMORI = "Kotatsu"

class ShikimoriInterceptor(private val storage: ShikimoriStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request().newBuilder()
		request.header(CommonHeaders.USER_AGENT, USER_AGENT_SHIKIMORI)
		storage.accessToken?.let {
			request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
		}
		val response = chain.proceed(request.build())
		if (!response.isSuccessful && !response.isRedirect) {
			throw IOException("${response.code} ${response.message}")
		}
		return response
	}
}