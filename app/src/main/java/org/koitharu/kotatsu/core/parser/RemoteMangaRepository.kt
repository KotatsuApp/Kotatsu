package org.koitharu.kotatsu.core.parser

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.cache.ContentCache
import org.koitharu.kotatsu.core.cache.SafeDeferred
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.Favicons
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.domain
import org.koitharu.kotatsu.utils.ext.processLifecycleScope
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable

class RemoteMangaRepository(
	private val parser: MangaParser,
	private val cache: ContentCache,
) : MangaRepository, Interceptor {

	override val source: MangaSource
		get() = parser.source

	override val sortOrders: Set<SortOrder>
		get() = parser.sortOrders

	var defaultSortOrder: SortOrder?
		get() = getConfig().defaultSortOrder ?: sortOrders.firstOrNull()
		set(value) {
			getConfig().defaultSortOrder = value
		}

	val domain: String
		get() = parser.domain

	val headers: Headers?
		get() = parser.headers

	override fun intercept(chain: Interceptor.Chain): Response {
		return if (parser is Interceptor) {
			parser.intercept(chain)
		} else {
			chain.proceed(chain.request())
		}
	}

	override suspend fun getList(offset: Int, query: String): List<Manga> {
		return parser.getList(offset, query)
	}

	override suspend fun getList(offset: Int, tags: Set<MangaTag>?, sortOrder: SortOrder?): List<Manga> {
		return parser.getList(offset, tags, sortOrder)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		cache.getDetails(source, manga.url)?.let { return it }
		val details = asyncSafe {
			parser.getDetails(manga)
		}
		cache.putDetails(source, manga.url, details)
		return details.await()
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		cache.getPages(source, chapter.url)?.let { return it }
		val pages = asyncSafe {
			parser.getPages(chapter)
		}
		cache.putPages(source, chapter.url, pages)
		return pages.await()
	}

	override suspend fun getPageUrl(page: MangaPage): String = parser.getPageUrl(page)

	override suspend fun getTags(): Set<MangaTag> = parser.getTags()

	suspend fun getFavicons(): Favicons = parser.getFavicons()

	fun getAuthProvider(): MangaParserAuthProvider? = parser as? MangaParserAuthProvider

	fun getConfigKeys(): List<ConfigKey<*>> = ArrayList<ConfigKey<*>>().also {
		parser.onCreateConfig(it)
	}

	private fun getConfig() = parser.config as SourceSettings

	private suspend fun <T> asyncSafe(block: suspend CoroutineScope.() -> T): SafeDeferred<T> {
		var dispatcher = currentCoroutineContext()[CoroutineDispatcher.Key]
		if (dispatcher == null || dispatcher is MainCoroutineDispatcher) {
			dispatcher = Dispatchers.Default
		}
		return SafeDeferred(
			processLifecycleScope.async(dispatcher) {
				runCatchingCancellable { block() }
			},
		)
	}
}
