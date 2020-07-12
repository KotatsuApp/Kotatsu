package org.koitharu.kotatsu.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.SortOrder
import java.util.*

class MangaSearchRepository : KoinComponent {

	fun globalSearch(query: String, batchSize: Int = 4): Flow<List<Manga>> = flow {
		val sources = MangaProviderFactory.getSources(false)
		val lists = EnumMap<MangaSource, List<Manga>>(MangaSource::class.java)
		var i = 0
		while (true) {
			var isEmitted = false
			for (source in sources) {
				val list = lists.getOrPut(source) {
					try {
						MangaProviderFactory.create(source).getList(0, query, SortOrder.POPULARITY)
					} catch (e: Throwable) {
						e.printStackTrace()
						emptyList<Manga>()
					}
				}
				if (i < list.size) {
					emit(list.subList(i, (i + batchSize).coerceAtMost(list.lastIndex)))
					isEmitted = true
				}
			}
			i += batchSize
			if (!isEmitted) {
				return@flow
			}
		}
	}
}