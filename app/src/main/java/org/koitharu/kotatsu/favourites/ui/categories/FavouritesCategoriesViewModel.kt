package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository

class FavouritesCategoriesViewModel(
	private val repository: FavouritesRepository
) : BaseViewModel() {

	private var reorderJob: Job? = null

	val categories = repository.observeCategories()
		.flowOn(Dispatchers.Default).asLiveData(viewModelScope.coroutineContext)

	fun createCategory(name: String) {
		launchJob(Dispatchers.Default) {
			repository.addCategory(name)
		}
	}

	fun renameCategory(id: Long, name: String) {
		launchJob(Dispatchers.Default) {
			repository.renameCategory(id, name)
		}
	}

	fun deleteCategory(id: Long) {
		launchJob(Dispatchers.Default) {
			repository.removeCategory(id)
		}
	}

	fun reorderCategories(oldPos: Int, newPos: Int) {
		val prevJob = reorderJob
		reorderJob = launchJob(Dispatchers.Default) {
			prevJob?.join()
			val items = categories.value ?: error("This should not happen")
			val ids = items.mapTo(ArrayList(items.size)) { it.id }
			val item = ids.removeAt(oldPos)
			ids.add(newPos, item)
			repository.reorderCategories(ids)
		}
	}
}