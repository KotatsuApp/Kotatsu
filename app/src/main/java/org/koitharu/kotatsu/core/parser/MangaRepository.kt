package org.koitharu.kotatsu.core.parser

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import org.koitharu.kotatsu.core.model.*

interface MangaRepository {

	val sortOrders: Set<SortOrder>

	suspend fun getList(
		offset: Int,
		query: String? = null,
		tags: Set<MangaTag>? = null,
		sortOrder: SortOrder? = null,
	): List<Manga> = if (tags == null || tags.size <= 1) {
		getList(offset, query, sortOrder, tags?.singleOrNull())
	} else {
		throw NotImplementedError("Multiple filter are not supported by this source yet")
	}

	@Deprecated("Use multiple tag variant")
	suspend fun getList(
		offset: Int,
		query: String? = null,
		sortOrder: SortOrder? = null,
		tag: MangaTag? = null,
	): List<Manga> = throw NotImplementedError("This is fine")

	suspend fun getDetails(manga: Manga): Manga

	suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	suspend fun getPageUrl(page: MangaPage): String

	suspend fun getTags(): Set<MangaTag>

	companion object : KoinComponent {

		operator fun invoke(source: MangaSource): MangaRepository {
			return get(named(source))
		}
	}
}