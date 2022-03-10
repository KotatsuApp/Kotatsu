package org.koitharu.kotatsu.shikimori.data

import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.network.CommonHeaders

private const val USER_AGENT_SHIKIMORI = "Kotatsu"

class ShikimoriInterceptor(private val storage: ShikimoriStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request().newBuilder()
		request.header(CommonHeaders.USER_AGENT, USER_AGENT_SHIKIMORI)
		storage.accessToken?.let {
			request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
		}
		return chain.proceed(request.build())
	}
}