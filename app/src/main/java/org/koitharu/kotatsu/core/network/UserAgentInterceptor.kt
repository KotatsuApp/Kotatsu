package org.koitharu.kotatsu.core.network

import android.os.Build
import dagger.Lazy
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAgentInterceptor @Inject constructor(
	private val mangaRepositoryFactoryLazy: Lazy<MangaRepository.Factory>,
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		return chain.proceed(
			if (request.header(CommonHeaders.USER_AGENT) == null) {
				request.newBuilder()
					.addHeader(CommonHeaders.USER_AGENT, getUserAgent(request))
					.build()
			} else request,
		)
	}

	private fun getUserAgent(request: Request): String {
		val source = request.tag(MangaSource::class.java) ?: return userAgent
		val repository = mangaRepositoryFactoryLazy.get().create(source) as? RemoteMangaRepository
		return repository?.userAgent ?: userAgent
	}

	companion object {

		val userAgent
			get() = "Kotatsu/%s (Android %s; %s; %s %s; %s)".format(
				BuildConfig.VERSION_NAME,
				Build.VERSION.RELEASE,
				Build.MODEL,
				Build.BRAND,
				Build.DEVICE,
				Locale.getDefault().language,
			) // TODO Decide what to do with this afterwards

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
