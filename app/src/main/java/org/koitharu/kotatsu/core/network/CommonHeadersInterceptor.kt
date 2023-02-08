package org.koitharu.kotatsu.core.network

import android.os.Build
import android.util.Log
import dagger.Lazy
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mergeWith
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonHeadersInterceptor @Inject constructor(
	private val mangaRepositoryFactoryLazy: Lazy<MangaRepository.Factory>,
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val source = request.tag(MangaSource::class.java)
		val repository = if (source != null) {
			mangaRepositoryFactoryLazy.get().create(source) as? RemoteMangaRepository
		} else {
			if (BuildConfig.DEBUG) {
				Log.w("Http", "Request without source tag: ${request.url}")
			}
			null
		}
		val headersBuilder = request.headers.newBuilder()
		repository?.headers?.let {
			headersBuilder.mergeWith(it, replaceExisting = false)
		}
		if (headersBuilder[CommonHeaders.USER_AGENT] == null) {
			headersBuilder[CommonHeaders.USER_AGENT] = userAgentFallback
		}
		if (headersBuilder[CommonHeaders.REFERER] == null && repository != null) {
			headersBuilder[CommonHeaders.REFERER] = "https://${repository.domain}/"
		}
		val newRequest = request.newBuilder().headers(headersBuilder.build()).build()
		return repository?.intercept(ProxyChain(chain, newRequest)) ?: chain.proceed(newRequest)
	}

	private class ProxyChain(
		private val delegate: Interceptor.Chain,
		private val request: Request,
	) : Interceptor.Chain by delegate {

		override fun request(): Request = request
	}

	companion object {

		val userAgentFallback
			get() = "Kotatsu/%s (Android %s; %s; %s %s; %s)".format(
				BuildConfig.VERSION_NAME,
				Build.VERSION.RELEASE,
				Build.MODEL,
				Build.BRAND,
				Build.DEVICE,
				Locale.getDefault().language,
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
