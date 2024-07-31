package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.EnumSet
import java.util.Locale

class EmptyMangaRepository(override val source: MangaSource) : MangaRepository {

	override val sortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)
	override val states: Set<MangaState>
		get() = emptySet()
	override val contentRatings: Set<ContentRating>
		get() = emptySet()
	override var defaultSortOrder: SortOrder
		get() = SortOrder.NEWEST
		set(value) = Unit
	override val isMultipleTagsSupported: Boolean
		get() = false
	override val isTagsExclusionSupported: Boolean
		get() = false
	override val isSearchSupported: Boolean
		get() = false

	override suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga> = stub(null)

	override suspend fun getDetails(manga: Manga): Manga = stub(manga)

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = stub(null)

	override suspend fun getPageUrl(page: MangaPage): String = stub(null)

	override suspend fun getTags(): Set<MangaTag> = stub(null)

	override suspend fun getLocales(): Set<Locale> = stub(null)

	override suspend fun getRelated(seed: Manga): List<Manga> = stub(seed)

	private fun stub(manga: Manga?): Nothing {
		throw UnsupportedSourceException("This manga source is not supported", manga)
	}
}
