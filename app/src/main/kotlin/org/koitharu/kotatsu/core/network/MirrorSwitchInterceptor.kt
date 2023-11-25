package org.koitharu.kotatsu.core.network

import androidx.collection.ArraySet
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
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
import java.util.EnumMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MirrorSwitchInterceptor @Inject constructor(
	private val mangaRepositoryFactoryLazy: Lazy<MangaRepository.Factory>,
	private val settings: AppSettings,
) : Interceptor {

	private val locks = EnumMap<MangaSource, Any>(MangaSource::class.java)
	private val blacklist = EnumMap<MangaSource, MutableSet<String>>(MangaSource::class.java)

	val isEnabled: Boolean
		get() = settings.isMirrorSwitchingAvailable

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		if (!isEnabled) {
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

	suspend fun trySwitchMirror(repository: RemoteMangaRepository): Boolean = runInterruptible(Dispatchers.Default) {
		if (!isEnabled) {
			return@runInterruptible false
		}
		val mirrors = repository.getAvailableMirrors()
		if (mirrors.size <= 1) {
			return@runInterruptible false
		}
		synchronized(obtainLock(repository.source)) {
			val currentMirror = repository.domain
			if (currentMirror !in mirrors) {
				return@synchronized false
			}
			addToBlacklist(repository.source, currentMirror)
			val newMirror = mirrors.firstOrNull { x ->
				x != currentMirror && !isBlacklisted(repository.source, x)
			} ?: return@synchronized false
			repository.domain = newMirror
			true
		}
	}

	fun rollback(repository: RemoteMangaRepository, oldMirror: String) = synchronized(obtainLock(repository.source)) {
		blacklist[repository.source]?.remove(oldMirror)
		repository.domain = oldMirror
	}

	private fun trySwitchMirror(request: Request, chain: Interceptor.Chain): Response? {
		val source = request.tag(MangaSource::class.java) ?: return null
		val repository = mangaRepositoryFactoryLazy.get().create(source) as? RemoteMangaRepository ?: return null
		val mirrors = repository.getAvailableMirrors()
		if (mirrors.isEmpty()) {
			return null
		}
		return synchronized(obtainLock(repository.source)) {
			tryMirrors(repository, mirrors, chain, request)
		}
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
			if (mirror == currentDomain || isBlacklisted(repository.source, mirror)) {
				continue
			}
			val newHost = hostOf(url.host, mirror) ?: continue
			val newRequest = request.newBuilder()
				.url(urlBuilder.host(newHost).build())
				.build()
			val response = chain.proceed(newRequest)
			if (response.isFailed) {
				addToBlacklist(repository.source, mirror)
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

	private fun obtainLock(source: MangaSource): Any = locks.getOrPut(source) {
		Any()
	}

	private fun isBlacklisted(source: MangaSource, domain: String): Boolean {
		return blacklist[source]?.contains(domain) == true
	}

	private fun addToBlacklist(source: MangaSource, domain: String) {
		blacklist.getOrPut(source) {
			ArraySet(2)
		}.add(domain)
	}
}
