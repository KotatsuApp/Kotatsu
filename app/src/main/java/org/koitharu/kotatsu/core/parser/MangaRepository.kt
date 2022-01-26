package org.koitharu.kotatsu.core.parser

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import org.koitharu.kotatsu.core.model.*

interface MangaRepository {

	val source: MangaSource

	val sortOrders: Set<SortOrder>

	suspend fun getList2(
		offset: Int,
		query: String? = null,
		tags: Set<MangaTag>? = null,
		sortOrder: SortOrder? = null,
	): List<Manga>

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