package org.koitharu.kotatsu.core.parser.external

import android.content.ContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.EnumSet

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

	override val filterCapabilities: MangaListFilterCapabilities
		get() = capabilities.let {
			MangaListFilterCapabilities(
				isMultipleTagsSupported = it?.isMultipleTagsSupported == true,
				isTagsExclusionSupported = it?.isTagsExclusionSupported == true,
				isSearchSupported = it?.isSearchSupported == true,
				isSearchWithFiltersSupported = false, // TODO
				isYearSupported = false, // TODO
				isYearRangeSupported = false, // TODO
				isOriginalLocaleSupported = false, // TODO
			)
		}

	override var defaultSortOrder: SortOrder
		get() = capabilities?.defaultSortOrder ?: SortOrder.ALPHABETICAL
		set(value) = Unit

	override suspend fun getFilterOptions(): MangaListFilterOptions = capabilities.let {
		MangaListFilterOptions(
			availableTags = runInterruptible(Dispatchers.IO) {
				contentSource.getTags()
			},
			availableStates = it?.availableStates.orEmpty(),
			availableContentRating = it?.availableContentRating.orEmpty(),
			availableContentTypes = emptySet(),
			availableDemographics = emptySet(),
			availableLocales = emptySet(),
		)
	}

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> =
		runInterruptible(Dispatchers.IO) {
			contentSource.getList(offset, order ?: defaultSortOrder, filter ?: MangaListFilter.EMPTY)
		}

	override suspend fun getDetailsImpl(manga: Manga): Manga = runInterruptible(Dispatchers.IO) {
		contentSource.getDetails(manga)
	}

	override suspend fun getPagesImpl(chapter: MangaChapter): List<MangaPage> = runInterruptible(Dispatchers.IO) {
		contentSource.getPages(chapter)
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url // TODO

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = emptyList() // TODO
}
