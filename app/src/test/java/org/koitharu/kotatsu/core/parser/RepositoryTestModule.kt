package org.koitharu.kotatsu.parsers

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.koin.dsl.module
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.network.TestCookieJar
import org.koitharu.kotatsu.core.network.UserAgentInterceptor
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.parser.SourceSettingsStub
import org.koitharu.kotatsu.core.prefs.SourceSettings
import java.util.concurrent.TimeUnit

val repositoryTestModule
	get() = module {
		single<CookieJar> { TestCookieJar() }
		factory {
			OkHttpClient.Builder()
				.cookieJar(get())
				.addInterceptor(UserAgentInterceptor())
				.connectTimeout(20, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(20, TimeUnit.SECONDS)
				.build()
		}
		single<MangaLoaderContext> {
			object : MangaLoaderContext(get(), get()) {
				override fun getSettings(source: MangaSource): SourceSettings {
					return SourceSettingsStub()
				}
			}
		}
		factory { (source: MangaSource) ->
			runCatching {
				source.cls.getDeclaredConstructor(MangaLoaderContext::class.java)
					.newInstance(get<MangaLoaderContext>())
			}.recoverCatching {
				source.cls.newInstance()
			}.getOrThrow() as RemoteMangaRepository
		}
	}