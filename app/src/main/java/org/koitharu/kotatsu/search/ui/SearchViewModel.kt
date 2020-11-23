package org.koitharu.kotatsu.search.ui

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel

class SearchViewModel(
	private val repository: MangaRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	override val content = MutableLiveData<List<Any>>()

	fun loadList(query: String, append: Boolean) {
		launchLoadingJob {
			val list = withContext(Dispatchers.Default) {
				repository.getList(TODO(), query = query)
			}
			if (!append) {
				content.value = list
			} else {
				content.value = content.value.orEmpty() + list
			}
		}
	}
}