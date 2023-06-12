package org.koitharu.kotatsu.core.network

import dagger.Lazy
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.canParseAsIpAddress
import okhttp3.internal.closeQuietly
import okhttp3.internal.publicsuffix.PublicSuffixDatabase
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MirrorSwitchInterceptor @Inject constructor(
	private val mangaRepositoryFactoryLazy: Lazy<MangaRepository.Factory>,
	private val settings: AppSettings,
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		if (!settings.isMirrorSwitchingAvailable) {
			return chain.proceed(request)
		}
		return try {
			val response = chain.proceed(request)
			if (response.isFailed) {
				val responseCopy = response.copy()
				response.closeQuietly()
				trySwitchMirror(request, chain)?.also {
					responseCopy.closeQuietly()
				} ?: responseCopy
			} else {
				response
			}
		} catch (e: Exception) {
			trySwitchMirror(request, chain) ?: throw e
		}
	}

	private fun trySwitchMirror(request: Request, chain: Interceptor.Chain): Response? {
		val source = request.tag(MangaSource::class.java) ?: return null
		val repository = mangaRepositoryFactoryLazy.get().create(source) as? RemoteMangaRepository ?: return null
		val mirrors = repository.getAvailableMirrors()
		if (mirrors.isEmpty()) {
			return null
		}
		return tryMirrors(repository, mirrors, chain, request)
	}

	private fun tryMirrors(
		repository: RemoteMangaRepository,
		mirrors: List<String>,
		chain: Interceptor.Chain,
		request: Request,
	): Response? {
		val url = request.url
		val currentDomain = url.topPrivateDomain()
		if (currentDomain !in mirrors) {
			return null
		}
		val urlBuilder = url.newBuilder()
		for (mirror in mirrors) {
			if (mirror == currentDomain) {
				continue
			}
			val newHost = hostOf(url.host, mirror) ?: continue
			val newRequest = request.newBuilder()
				.url(urlBuilder.host(newHost).build())
				.build()
			val response = chain.proceed(newRequest)
			if (response.isFailed) {
				response.closeQuietly()
			} else {
				repository.domain = mirror
				return response
			}
		}
		return null
	}

	private val Response.isFailed: Boolean
		get() = code in 400..599

	private fun hostOf(host: String, newDomain: String): String? {
		if (newDomain.canParseAsIpAddress()) {
			return newDomain
		}
		val domain = PublicSuffixDatabase.get().getEffectiveTldPlusOne(host) ?: return null
		return host.removeSuffix(domain) + newDomain
	}

	private fun Response.copy(): Response {
		return newBuilder()
			.body(body?.copy())
			.build()
	}

	private fun ResponseBody.copy(): ResponseBody {
		return source().readByteArray().toResponseBody(contentType())
	}
}
