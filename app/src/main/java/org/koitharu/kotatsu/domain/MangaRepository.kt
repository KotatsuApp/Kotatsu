package org.koitharu.kotatsu.domain

import org.koitharu.kotatsu.core.model.*

abstract class MangaRepository(protected val loaderContext: MangaLoaderContext) {

	open val sortOrders: Set<SortOrder> get() = emptySet()

	open val isSearchAvailable get() = true

	abstract suspend fun getList(offset: Int, query: String? = null, sortOrder: SortOrder? = null, tags: Set<String>? = null): List<Manga>

	abstract suspend fun getDetails(manga: Manga) : Manga

	abstract suspend fun getPages(chapter: MangaChapter) : List<MangaPage>

	open suspend fun getPageFullUrl(page: MangaPage) : String = page.url
}