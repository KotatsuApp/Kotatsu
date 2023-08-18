package org.koitharu.kotatsu.core.parser

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.cache.ContentCache
import org.koitharu.kotatsu.core.cache.SafeDeferred
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
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
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable

class RemoteMangaRepository(
	private val parser: MangaParser,
	private val cache: ContentCache,
) : MangaRepository, Interceptor {

	override val source: MangaSource
		get() = parser.source

	override val sortOrders: Set<SortOrder>
		get() = parser.sortOrders

	override var defaultSortOrder: SortOrder
		get() = getConfig().defaultSortOrder ?: sortOrders.first()
		set(value) {
			getConfig().defaultSortOrder = value
		}

	var domain: String
		get() = parser.domain
		set(value) {
			getConfig()[parser.configKeyDomain] = value
		}

	val domains: Array<out String>
		get() = parser.configKeyDomain.presetValues

	val headers: Headers
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

	override suspend fun getDetails(manga: Manga): Manga = getDetails(manga, withCache = true)

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		cache.getPages(source, chapter.url)?.let { return it }
		val pages = asyncSafe {
			parser.getPages(chapter).distinctById()
		}
		cache.putPages(source, chapter.url, pages)
		return pages.await()
	}

	override suspend fun getPageUrl(page: MangaPage): String = parser.getPageUrl(page)

	override suspend fun getTags(): Set<MangaTag> = parser.getTags()

	suspend fun getFavicons(): Favicons = parser.getFavicons()

	override suspend fun getRelated(seed: Manga): List<Manga> {
		cache.getRelatedManga(source, seed.url)?.let { return it }
		val related = asyncSafe {
			parser.getRelatedManga(seed).filterNot { it.id == seed.id }
		}
		cache.putRelatedManga(source, seed.url, related)
		return related.await()
	}

	suspend fun getDetails(manga: Manga, withCache: Boolean): Manga {
		if (!withCache) {
			return parser.getDetails(manga)
		}
		cache.getDetails(source, manga.url)?.let { return it }
		val details = asyncSafe {
			parser.getDetails(manga)
		}
		cache.putDetails(source, manga.url, details)
		return details.await()
	}

	suspend fun find(manga: Manga): Manga? {
		val list = getList(0, manga.title)
		return list.find { x -> x.id == manga.id }
	}

	fun getAuthProvider(): MangaParserAuthProvider? = parser as? MangaParserAuthProvider

	fun getConfigKeys(): List<ConfigKey<*>> = ArrayList<ConfigKey<*>>().also {
		parser.onCreateConfig(it)
	}

	fun getAvailableMirrors(): List<String> {
		return parser.configKeyDomain.presetValues.toList()
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

	private fun List<MangaPage>.distinctById(): List<MangaPage> {
		if (isEmpty()) {
			return emptyList()
		}
		val result = ArrayList<MangaPage>(size)
		val set = HashSet<Long>(size)
		for (page in this) {
			if (set.add(page.id)) {
				result.add(page)
			} else if (BuildConfig.DEBUG) {
				Log.w(null, "Duplicate page: $page")
			}
		}
		return result
	}
}
