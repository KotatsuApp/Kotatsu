package org.koitharu.kotatsu.domain

import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.model.SortOrder

abstract class BaseMangaRepository(protected val loaderContext: MangaLoaderContext) : MangaRepository {

	override val sortOrders: Set<SortOrder> get() = emptySet()

	override val isSearchAvailable get() = true

	override suspend fun getPageFullUrl(page: MangaPage) : String = page.url
}