package org.koitharu.kotatsu.core.network

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.koitharu.kotatsu.core.prefs.AppSettings

class ProxyAuthenticator(
	private val settings: AppSettings,
) : Authenticator {

	override fun authenticate(route: Route?, response: Response): Request? {
		val login = settings.proxyLogin ?: return null
		val password = settings.proxyPassword ?: return null
		val credential = Credentials.basic(login, password)
		return response.request.newBuilder()
			.header(CommonHeaders.PROXY_AUTHORIZATION, credential)
			.build()
	}
}
