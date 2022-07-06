package org.koitharu.kotatsu.core.network

import android.content.Context
import androidx.collection.ArrayMap
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaSource

class MirrorsInterceptor(
	private val context: Context,
) : Interceptor {

	private val mirrorsMap = ArrayMap<String, Mirrors>()

	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		return if (response.isServerError) {
			trySwitchMirror(chain) ?: response
		} else {
			response
		}
	}

	fun register(parser: MangaParser) {
		val configKeys = ArrayList<ConfigKey<*>>()
		parser.onCreateConfig(configKeys)
		for (key in configKeys) {
			if (key is ConfigKey.Domain) {
				val mirrors = key.presetValues ?: continue
				mirrorsMap[parser.getDomain()] = Mirrors(parser.source, mirrors)
			}
		}
	}

	private fun trySwitchMirror(chain: Interceptor.Chain): Response? {
		val url = chain.request().url
		var mirrors = mirrorsMap[url.host]
		val domain = if (mirrors != null) {
			url.host
		} else {
			mirrors = mirrorsMap[url.topPrivateDomain()]
			url.topPrivateDomain()
		}
		if (domain == null || mirrors == null) {
			return null
		}
		synchronized(mirrors) {
			for (mirror in mirrors.mirrors) {
				val request = chain.request()
					.newBuilder()
					.url(url.newBuilder().host(mirror).build())
					.build()
				val response = chain.proceed(request)
				if (!response.isServerError) {
					switchMirror(domain, mirrors.source, mirror)
					return response
				}
			}
			return null
		}
	}

	private fun switchMirror(oldDomain: String, source: MangaSource, newDomain: String) {
		val mirrors = mirrorsMap[oldDomain]?.mirrors?.toMutableList()
		if (mirrors != null) {
			mirrors.remove(newDomain)
			mirrors.add(oldDomain)
		}
		mirrorsMap[newDomain] = Mirrors(source, (mirrors ?: listOf(oldDomain)).toTypedArray())
		val settings = SourceSettings(context, source)
		settings[ConfigKey.Domain(oldDomain, null)] = newDomain
	}

	private val Response.isServerError: Boolean
		get() {
			return code in 500..599
		}

	private class Mirrors(
		val source: MangaSource,
		val mirrors: Array<String>,
	) {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Mirrors

			if (source != other.source) return false
			if (!mirrors.contentEquals(other.mirrors)) return false

			return true
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + mirrors.contentHashCode()
			return result
		}
	}
}