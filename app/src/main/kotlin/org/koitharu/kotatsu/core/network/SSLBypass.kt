package org.koitharu.kotatsu.core.network

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager")
fun OkHttpClient.Builder.bypassSSLErrors() = also { builder ->
	runCatching {
		val trustAllCerts = object : X509TrustManager {
			override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

			override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit

			override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
		}
		val sslContext = SSLContext.getInstance("SSL")
		sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())
		val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
		builder.sslSocketFactory(sslSocketFactory, trustAllCerts)
		builder.hostnameVerifier { _, _ -> true }
	}.onFailure {
		it.printStackTraceDebug()
	}
}
