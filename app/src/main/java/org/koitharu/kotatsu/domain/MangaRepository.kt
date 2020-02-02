package org.koitharu.kotatsu.domain

import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.model.SortOrder

interface MangaRepository {

	val sortOrders: Set<SortOrder>

	val isSearchAvailable: Boolean

	suspend fun getList(offset: Int, query: String? = null, sortOrder: SortOrder? = null, tags: Set<String>? = null): List<Manga>

	suspend fun getDetails(manga: Manga) : Manga

	suspend fun getPages(chapter: MangaChapter) : List<MangaPage>

	suspend fun getPageFullUrl(page: MangaPage) : String
}