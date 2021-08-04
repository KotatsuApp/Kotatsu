package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import java.util.*

class FavouritesCategoriesViewModel(
	private val repository: FavouritesRepository
) : BaseViewModel() {

	private var reorderJob: Job? = null

	val categories = repository.observeCategories()
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	fun createCategory(name: String) {
		launchJob {
			repository.addCategory(name)
		}
	}

	fun renameCategory(id: Long, name: String) {
		launchJob {
			repository.renameCategory(id, name)
		}
	}

	fun deleteCategory(id: Long) {
		launchJob {
			repository.removeCategory(id)
		}
	}

	fun setCategoryOrder(id: Long, order: SortOrder) {
		launchJob {
			repository.setCategoryOrder(id, order)
		}
	}

	fun reorderCategories(oldPos: Int, newPos: Int) {
		val prevJob = reorderJob
		reorderJob = launchJob(Dispatchers.Default) {
			prevJob?.join()
			val items = categories.value ?: error("This should not happen")
			val ids = items.mapTo(ArrayList(items.size)) { it.id }
			Collections.swap(ids, oldPos, newPos)
			repository.reorderCategories(ids)
		}
	}
}