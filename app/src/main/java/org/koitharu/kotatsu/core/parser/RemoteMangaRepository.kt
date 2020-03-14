package org.koitharu.kotatsu.core.parser

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.domain.MangaLoaderContext

abstract class RemoteMangaRepository : MangaRepository, KoinComponent {

	protected abstract val source: MangaSource

	protected val loaderContext by inject<MangaLoaderContext>()
	protected val conf by lazy(LazyThreadSafetyMode.NONE) {
		loaderContext.getSettings(source)
	}

	override val sortOrders: Set<SortOrder> get() = emptySet()

	override suspend fun getPageFullUrl(page: MangaPage): String = page.url

	override suspend fun getTags(): Set<MangaTag> = emptySet()

	abstract fun onCreatePreferences(): Set<Int>
}