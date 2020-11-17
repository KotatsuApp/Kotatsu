package org.koitharu.kotatsu.remotelist.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.list.ui.MangaFilterConfig
import org.koitharu.kotatsu.list.ui.MangaListViewModel

class RemoteListViewModel(
	private val repository: MangaRepository
) : MangaListViewModel() {

	private var appliedFilter: MangaFilter? = null

	init {
		loadFilter()
	}

	fun loadList(offset: Int) {
		launchLoadingJob {
			val list = withContext(Dispatchers.Default) {
				repository.getList(
					offset = offset,
					sortOrder = appliedFilter?.sortOrder,
					tag = appliedFilter?.tag
				)
			}
			if (offset == 0) {
				content.value = list
			} else {
				content.value = content.value.orEmpty() + list
			}
		}
	}

	fun applyFilter(newFilter: MangaFilter) {
		appliedFilter = newFilter
		content.value = emptyList()
		loadList(0)
	}

	private fun loadFilter() {
		launchJob {
			try {
				val (sorts, tags) = withContext(Dispatchers.Default) {
					repository.sortOrders.sortedBy { it.ordinal } to repository.getTags()
						.sortedBy { it.title }
				}
				filter.value = MangaFilterConfig(sorts, tags, appliedFilter)
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
			}
		}
	}
}