package org.koitharu.kotatsu.core.parser

import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.InteractiveActionRequiredException
import org.koitharu.kotatsu.core.exceptions.ProxyConfigException
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.Favicons
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy

class ParserMangaRepository(
	private val parser: MangaParser,
	private val mirrorSwitcher: MirrorSwitcher,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache), Interceptor {

	private val filterOptionsLazy = suspendLazy(Dispatchers.Default) {
		withMirrors {
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

	override fun intercept(chain: Interceptor.Chain): Response = parser.intercept(chain)

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> {
		return withMirrors {
			parser.getList(offset, order ?: defaultSortOrder, filter ?: MangaListFilter.EMPTY)
		}
	}

	override suspend fun getPagesImpl(
		chapter: MangaChapter
	): List<MangaPage> = withMirrors {
		parser.getPages(chapter)
	}

	override suspend fun getPageUrl(page: MangaPage): String = withMirrors {
		parser.getPageUrl(page).also { result ->
			check(result.isNotEmpty()) { "Page url is empty" }
		}
	}

	override suspend fun getFilterOptions(): MangaListFilterOptions = filterOptionsLazy.get()

	suspend fun getFavicons(): Favicons = withMirrors {
		parser.getFavicons()
	}

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = parser.getRelatedManga(seed)

	override suspend fun getDetailsImpl(manga: Manga): Manga = withMirrors {
		parser.getDetails(manga)
	}

	fun getAuthProvider(): MangaParserAuthProvider? = parser.authorizationProvider

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

	private suspend fun <T : Any> withMirrors(block: suspend () -> T): T {
		if (!mirrorSwitcher.isEnabled) {
			return block()
		}
		val initialResult = runCatchingCancellable { block() }
		if (initialResult.isValidResult()) {
			return initialResult.getOrThrow()
		}
		val newResult = mirrorSwitcher.trySwitchMirror(this, block)
		return newResult ?: initialResult.getOrThrow()
	}

	private fun Result<Any>.isValidResult() = fold(
		onSuccess = {
			when (it) {
				is Collection<*> -> it.isNotEmpty()
				else -> true
			}
		},
		onFailure = {
			when (it.cause) {
				is CloudFlareProtectedException,
				is AuthRequiredException,
				is InteractiveActionRequiredException,
				is ProxyConfigException -> true

				else -> false
			}
		},
	)
}
