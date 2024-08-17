package org.koitharu.kotatsu.core.parser

import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.network.MirrorSwitchInterceptor
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Favicons
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.domain
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.util.Locale

class ParserMangaRepository(
	private val parser: MangaParser,
	private val mirrorSwitchInterceptor: MirrorSwitchInterceptor,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache), Interceptor {

	override val source: MangaParserSource
		get() = parser.source

	override val sortOrders: Set<SortOrder>
		get() = parser.availableSortOrders

	override val states: Set<MangaState>
		get() = parser.availableStates

	override val contentRatings: Set<ContentRating>
		get() = parser.availableContentRating

	override var defaultSortOrder: SortOrder
		get() = getConfig().defaultSortOrder ?: sortOrders.first()
		set(value) {
			getConfig().defaultSortOrder = value
		}

	override val isMultipleTagsSupported: Boolean
		get() = parser.isMultipleTagsSupported

	override val isSearchSupported: Boolean
		get() = parser.isSearchSupported

	override val isTagsExclusionSupported: Boolean
		get() = parser.isTagsExclusionSupported

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

	override suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga> {
		return mirrorSwitchInterceptor.withMirrorSwitching {
			parser.getList(offset, filter)
		}
	}

	override suspend fun getPagesImpl(
		chapter: MangaChapter
	): List<MangaPage> = mirrorSwitchInterceptor.withMirrorSwitching {
		parser.getPages(chapter)
	}

	override suspend fun getPageUrl(page: MangaPage): String = mirrorSwitchInterceptor.withMirrorSwitching {
		parser.getPageUrl(page)
	}

	override suspend fun getTags(): Set<MangaTag> = mirrorSwitchInterceptor.withMirrorSwitching {
		parser.getAvailableTags()
	}

	override suspend fun getLocales(): Set<Locale> {
		return parser.getAvailableLocales()
	}

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
