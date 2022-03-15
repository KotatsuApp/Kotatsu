package org.koitharu.kotatsu.core.network

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.parser.MangaLoaderContextImpl
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.utils.DownloadManagerHelper
import java.util.concurrent.TimeUnit

val networkModule
	get() = module {
		single { AndroidCookieJar() } bind CookieJar::class
		single {
			OkHttpClient.Builder().apply {
				connectTimeout(20, TimeUnit.SECONDS)
				readTimeout(60, TimeUnit.SECONDS)
				writeTimeout(20, TimeUnit.SECONDS)
				cookieJar(get())
				cache(get<LocalStorageManager>().createHttpCache())
				addInterceptor(UserAgentInterceptor())
				addInterceptor(CloudFlareInterceptor())
				if (BuildConfig.DEBUG) {
					addNetworkInterceptor(CurlLoggingInterceptor())
				}
			}.build()
		}
		factory { DownloadManagerHelper(get(), get()) }
		single<MangaLoaderContext> { MangaLoaderContextImpl(get(), get(), get()) }
	}
