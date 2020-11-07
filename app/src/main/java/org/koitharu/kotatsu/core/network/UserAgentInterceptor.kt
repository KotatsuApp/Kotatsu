package org.koitharu.kotatsu.core.network

import android.os.Build
import okhttp3.Interceptor
import org.koitharu.kotatsu.BuildConfig
import java.util.*

class UserAgentInterceptor : Interceptor {

	private val userAgent = "Kotatsu/%s (Android %s; %s; %s %s; %s)".format(
		BuildConfig.VERSION_NAME,
		Build.VERSION.RELEASE,
		Build.MODEL,
		Build.BRAND,
		Build.DEVICE,
		Locale.getDefault().language
	)

	override fun intercept(chain: Interceptor.Chain) = chain.proceed(
		chain.request().newBuilder()
			.header("User-Agent", userAgent)
			.build()
	)
}