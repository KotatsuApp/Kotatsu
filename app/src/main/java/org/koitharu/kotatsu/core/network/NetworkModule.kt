package org.koitharu.kotatsu.core.network

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.MangaLoaderContextImpl
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import java.util.concurrent.TimeUnit

val networkModule
	get() = module {
		single { AndroidCookieJar() } bind CookieJar::class
		single {
			val cache = get<LocalStorageManager>().createHttpCache()
			OkHttpClient.Builder().apply {
				connectTimeout(20, TimeUnit.SECONDS)
				readTimeout(60, TimeUnit.SECONDS)
				writeTimeout(20, TimeUnit.SECONDS)
				cookieJar(get())
				dns(DoHManager(cache, get()))
				cache(cache)
				addInterceptor(GZipInterceptor())
				addInterceptor(UserAgentInterceptor())
				addInterceptor(CloudFlareInterceptor())
			}.build()
		}
		single<MangaLoaderContext> { MangaLoaderContextImpl(get(), get(), get()) }
	}
