package org.koitharu.kotatsu.scrobbling.shikimori.data

import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.network.CommonHeaders

class ShikimoriAuthenticator @Inject constructor(
	private val storage: ShikimoriStorage,
	private val repositoryProvider: Provider<ShikimoriRepository>,
) : Authenticator {

	override fun authenticate(route: Route?, response: Response): Request? {
		val accessToken = storage.accessToken ?: return null
		if (!isRequestWithAccessToken(response)) {
			return null
		}
		synchronized(this) {
			val newAccessToken = storage.accessToken ?: return null
			if (accessToken != newAccessToken) {
				return newRequestWithAccessToken(response.request, newAccessToken)
			}
			val updatedAccessToken = refreshAccessToken() ?: return null
			return newRequestWithAccessToken(response.request, updatedAccessToken)
		}
	}

	private fun isRequestWithAccessToken(response: Response): Boolean {
		val header = response.request.header(CommonHeaders.AUTHORIZATION)
		return header?.startsWith("Bearer") == true
	}

	private fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
		return request.newBuilder()
			.header(CommonHeaders.AUTHORIZATION, "Bearer $accessToken")
			.build()
	}

	private fun refreshAccessToken(): String? = runCatching {
		val repository = repositoryProvider.get()
		runBlocking { repository.authorize(null) }
		return storage.accessToken
	}.onFailure {
		if (BuildConfig.DEBUG) {
			it.printStackTrace()
		}
	}.getOrNull()
}
