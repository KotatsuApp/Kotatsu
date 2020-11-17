package org.koitharu.kotatsu.search.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel

class SearchViewModel(
	private val repository: MangaRepository
) : MangaListViewModel() {

	fun loadList(query: String, offset: Int) {
		launchLoadingJob {
			val list = withContext(Dispatchers.Default) {
				repository.getList(offset, query = query)
			}
			if (offset == 0) {
				content.value = list
			} else {
				content.value = content.value.orEmpty() + list
			}
		}
	}
}