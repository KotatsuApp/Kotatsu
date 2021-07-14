package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.core.prefs.SourceSettings

abstract class RemoteMangaRepository(
	protected val loaderContext: MangaLoaderContext
) : MangaRepository {

	protected abstract val source: MangaSource

	protected abstract val defaultDomain: String

	private val conf by lazy {
		loaderContext.getSettings(source)
	}

	override val sortOrders: Set<SortOrder> get() = emptySet()

	override suspend fun getPageUrl(page: MangaPage): String = page.url.withDomain()

	override suspend fun getTags(): Set<MangaTag> = emptySet()

	open fun onCreatePreferences(map: MutableMap<String, Any>) {
		map[SourceSettings.KEY_DOMAIN] = defaultDomain
	}

	protected fun getDomain() = conf.getDomain(defaultDomain)

	protected fun String.withDomain(subdomain: String? = null) = when {
		this.startsWith("//") -> buildString {
			append("http")
			if (conf.isUseSsl(true)) {
				append('s')
			}
			append(":")
			append(this@withDomain)
		}
		this.startsWith("/") -> buildString {
			append("http")
			if (conf.isUseSsl(true)) {
				append('s')
			}
			append("://")
			if (subdomain != null) {
				append(subdomain)
				append('.')
			}
			append(conf.getDomain(defaultDomain))
			append(this@withDomain)
		}
		else -> this
	}

	protected fun generateUid(url: String): Long {
		var h = 1125899906842597L
		source.name.forEach { c ->
			h = 31 * h + c.toLong()
		}
		url.forEach { c ->
			h = 31 * h + c.toLong()
		}
		return h
	}

	protected fun generateUid(id: Long): Long {
		var h = 1125899906842597L
		source.name.forEach { c ->
			h = 31 * h + c.toLong()
		}
		h = 31 * h + id
		return h
	}

	protected fun parseFailed(message: String? = null): Nothing {
		throw ParseException(message)
	}
}