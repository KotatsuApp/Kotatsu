package org.koitharu.kotatsu.core.parser

import android.annotation.SuppressLint
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.BuildConfig
import java.util.*

@SuppressLint("ConstantLocale")
object UserAgentInterceptor : Interceptor {

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