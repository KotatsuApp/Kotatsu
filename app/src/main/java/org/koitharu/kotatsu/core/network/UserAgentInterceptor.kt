package org.koitharu.kotatsu.core.network

import android.os.Build
import java.util.*
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.BuildConfig

class UserAgentInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		return chain.proceed(
			if (request.header(CommonHeaders.USER_AGENT) == null) {
				request.newBuilder()
					.addHeader(CommonHeaders.USER_AGENT, userAgent)
					.build()
			} else request
		)
	}

	companion object {

		val userAgent
			get() = "Kotatsu/%s (Android %s; %s; %s %s; %s)".format(
				BuildConfig.VERSION_NAME,
				Build.VERSION.RELEASE,
				Build.MODEL,
				Build.BRAND,
				Build.DEVICE,
				Locale.getDefault().language
			)

		val userAgentChrome
			get() = (
				"Mozilla/5.0 (Linux; Android %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) " +
					"Chrome/100.0.4896.127 Mobile Safari/537.36"
				).format(
				Build.VERSION.RELEASE,
				Build.MODEL,
			)
	}
}