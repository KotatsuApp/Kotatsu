package org.koitharu.kotatsu.core.network

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.koitharu.kotatsu.core.prefs.AppSettings
import java.net.PasswordAuthentication
import java.net.Proxy

class ProxyAuthenticator(
	private val settings: AppSettings,
) : Authenticator, java.net.Authenticator() {

	init {
		setDefault(this)
	}

	override fun authenticate(route: Route?, response: Response): Request? {
		if (!isProxyEnabled()) {
			return null
		}
		if (response.request.header(CommonHeaders.PROXY_AUTHORIZATION) != null) {
			return null
		}
		val login = settings.proxyLogin ?: return null
		val password = settings.proxyPassword ?: return null
		val credential = Credentials.basic(login, password)
		return response.request.newBuilder()
			.header(CommonHeaders.PROXY_AUTHORIZATION, credential)
			.build()
	}

	override fun getPasswordAuthentication(): PasswordAuthentication? {
		if (!isProxyEnabled()) {
			return null
		}
		val login = settings.proxyLogin ?: return null
		val password = settings.proxyPassword ?: return null
		return PasswordAuthentication(login, password.toCharArray())
	}

	private fun isProxyEnabled() = settings.proxyType != Proxy.Type.DIRECT
}
