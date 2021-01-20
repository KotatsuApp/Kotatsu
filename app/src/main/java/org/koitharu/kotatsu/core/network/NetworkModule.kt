package org.koitharu.kotatsu.core.network

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.utils.CacheUtils
import java.util.concurrent.TimeUnit

val networkModule
	get() = module {
		single { AndroidCookieJar() } bind CookieJar::class
		single(named(CacheUtils.QUALIFIER_HTTP)) { CacheUtils.createHttpCache(androidContext()) }
		single {
			OkHttpClient.Builder().apply {
				connectTimeout(20, TimeUnit.SECONDS)
				readTimeout(60, TimeUnit.SECONDS)
				writeTimeout(20, TimeUnit.SECONDS)
				cookieJar(get())
				cache(get(named(CacheUtils.QUALIFIER_HTTP)))
				addInterceptor(UserAgentInterceptor())
				addInterceptor(CloudFlareInterceptor())
			}.build()
		}
	}