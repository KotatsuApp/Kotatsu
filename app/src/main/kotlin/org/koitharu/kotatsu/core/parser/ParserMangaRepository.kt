package org.koitharu.kotatsu.core.parser

import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.network.MirrorSwitchInterceptor
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.Favicons
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.domain
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy

class ParserMangaRepository(
	private val parser: MangaParser,
	private val mirrorSwitchInterceptor: MirrorSwitchInterceptor,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache), Interceptor {

	private val filterOptionsLazy = suspendLazy(Dispatchers.Default) {
		mirrorSwitchInterceptor.withMirrorSwitching {
			parser.getFilterOptions()
		}
	}

	override val source: MangaParserSource
		get() = parser.source

	override val sortOrders: Set<SortOrder>
		get() = parser.availableSortOrders

	override val filterCapabilities: MangaListFilterCapabilities
		get() = parser.filterCapabilities

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

	override fun intercept(chain: Interceptor.Chain): Response {
		return if (parser is Interceptor) {
			parser.intercept(chain)
		} else {
			chain.proceed(chain.request())
		}
	}

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> {
		return mirrorSwitchInterceptor.withMirrorSwitching {
			parser.getList(offset, order ?: defaultSortOrder, filter ?: MangaListFilter.EMPTY)
		}
	}

	override suspend fun getPagesImpl(
		chapter: MangaChapter
	): List<MangaPage> = mirrorSwitchInterceptor.withMirrorSwitching {
		parser.getPages(chapter)
	}

	override suspend fun getPageUrl(page: MangaPage): String = mirrorSwitchInterceptor.withMirrorSwitching {
		parser.getPageUrl(page).also { result ->
			check(result.isNotEmpty()) { "Page url is empty" }
		}
	}

	override suspend fun getFilterOptions(): MangaListFilterOptions = filterOptionsLazy.get()

	suspend fun getFavicons(): Favicons = mirrorSwitchInterceptor.withMirrorSwitching {
		parser.getFavicons()
	}

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = parser.getRelatedManga(seed)

	override suspend fun getDetailsImpl(manga: Manga): Manga = mirrorSwitchInterceptor.withMirrorSwitching {
		parser.getDetails(manga)
	}

	fun getAuthProvider(): MangaParserAuthProvider? = parser as? MangaParserAuthProvider

	fun getRequestHeaders() = parser.getRequestHeaders()

	fun getConfigKeys(): List<ConfigKey<*>> = ArrayList<ConfigKey<*>>().also {
		parser.onCreateConfig(it)
	}

	fun getAvailableMirrors(): List<String> {
		return parser.configKeyDomain.presetValues.toList()
	}

	fun isSlowdownEnabled(): Boolean {
		return getConfig().isSlowdownEnabled
	}

	fun getConfig() = parser.config as SourceSettings

	private suspend fun <R> MirrorSwitchInterceptor.withMirrorSwitching(block: suspend () -> R): R {
		if (!isEnabled) {
			return block()
		}
		val initialMirror = domain
		val result = runCatchingCancellable {
			block()
		}
		if (result.isValidResult()) {
			return result.getOrThrow()
		}
		return if (trySwitchMirror(this@ParserMangaRepository)) {
			val newResult = runCatchingCancellable {
				block()
			}
			if (newResult.isValidResult()) {
				return newResult.getOrThrow()
			} else {
				rollback(this@ParserMangaRepository, initialMirror)
				return result.getOrThrow()
			}
		} else {
			result.getOrThrow()
		}
	}

	private fun Result<*>.isValidResult() = isSuccess && (getOrNull() as? Collection<*>)?.isEmpty() != true
}
