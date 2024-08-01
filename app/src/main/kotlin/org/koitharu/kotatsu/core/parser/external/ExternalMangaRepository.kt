package org.koitharu.kotatsu.core.parser.external

import android.content.ContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.EnumSet
import java.util.Locale

class ExternalMangaRepository(
	private val contentResolver: ContentResolver,
	override val source: ExternalMangaSource,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache) {

	private val contentSource = ExternalPluginContentSource(contentResolver, source)

	private val capabilities by lazy {
		runCatching {
			contentSource.getCapabilities()
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	override val sortOrders: Set<SortOrder>
		get() = capabilities?.availableSortOrders ?: EnumSet.of(SortOrder.ALPHABETICAL)

	override val states: Set<MangaState>
		get() = capabilities?.availableStates.orEmpty()

	override val contentRatings: Set<ContentRating>
		get() = capabilities?.availableContentRating.orEmpty()

	override var defaultSortOrder: SortOrder
		get() = capabilities?.defaultSortOrder ?: SortOrder.ALPHABETICAL
		set(value) = Unit

	override val isMultipleTagsSupported: Boolean
		get() = capabilities?.isMultipleTagsSupported ?: true

	override val isTagsExclusionSupported: Boolean
		get() = capabilities?.isTagsExclusionSupported ?: false

	override val isSearchSupported: Boolean
		get() = capabilities?.isSearchSupported ?: true

	override suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga> =
		runInterruptible(Dispatchers.IO) {
			contentSource.getList(offset, filter)
		}

	override suspend fun getDetailsImpl(manga: Manga): Manga = runInterruptible(Dispatchers.IO) {
		contentSource.getDetails(manga)
	}

	override suspend fun getPagesImpl(chapter: MangaChapter): List<MangaPage> = runInterruptible(Dispatchers.IO) {
		contentSource.getPages(chapter)
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url // TODO

	override suspend fun getTags(): Set<MangaTag> = runInterruptible(Dispatchers.IO) {
		contentSource.getTags()
	}

	override suspend fun getLocales(): Set<Locale> = emptySet() // TODO

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = emptyList() // TODO
}
