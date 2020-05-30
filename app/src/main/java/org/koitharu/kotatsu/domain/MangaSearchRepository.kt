package org.koitharu.kotatsu.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.SortOrder

class MangaSearchRepository : KoinComponent {

	fun globalSearch(query: String): Flow<List<Manga>> = flow {
		val sources = MangaProviderFactory.getSources(false)
		for (source in sources) {
			val provider = MangaProviderFactory.create(source)
			val list = provider.getList(0, query, SortOrder.POPULARITY)
			emit(list.take(4))
		}
	}
}