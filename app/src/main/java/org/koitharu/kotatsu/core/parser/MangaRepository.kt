package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.core.model.*

interface MangaRepository {

	val sortOrders: Set<SortOrder>

	suspend fun getList(
		offset: Int,
		query: String? = null,
		sortOrder: SortOrder? = null,
		tag: MangaTag? = null
	): List<Manga>

	suspend fun getDetails(manga: Manga): Manga

	suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	suspend fun getPageRequest(page: MangaPage): RequestDraft

	suspend fun getTags(): Set<MangaTag>
}