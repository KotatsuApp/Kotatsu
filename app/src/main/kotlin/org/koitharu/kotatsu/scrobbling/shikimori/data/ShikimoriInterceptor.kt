package org.koitharu.kotatsu.scrobbling.shikimori.data

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.common.domain.ScrobblerAuthRequiredException
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import java.net.HttpURLConnection

private const val USER_AGENT_SHIKIMORI = "Kotatsu"

class ShikimoriInterceptor(private val storage: ScrobblerStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val sourceRequest = chain.request()
		val request = sourceRequest.newBuilder()
		request.header(CommonHeaders.USER_AGENT, USER_AGENT_SHIKIMORI)
		val isAuthRequest = sourceRequest.url.pathSegments.contains("oauth")
		if (!isAuthRequest) {
			storage.accessToken?.let {
				request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
			}
		}
		val response = chain.proceed(request.build())
		if (!isAuthRequest && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw ScrobblerAuthRequiredException(ScrobblerService.SHIKIMORI)
		}
		if (!response.isSuccessful && !response.isRedirect) {
			throw IOException("${response.code} ${response.message}")
		}
		return response
	}
}
