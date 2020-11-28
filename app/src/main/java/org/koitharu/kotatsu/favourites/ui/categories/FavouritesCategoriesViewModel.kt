package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.utils.ext.mapToSet

class FavouritesCategoriesViewModel(
	private val repository: FavouritesRepository
) : BaseViewModel() {

	private var reorderJob: Job? = null
	private var mangaSubscription: Job? = null

	val categories = repository.observeCategories()
		.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)
	val mangaCategories = MutableLiveData<Set<Long>>(emptySet())

	fun observeMangaCategories(mangaId: Long) {
		mangaSubscription?.cancel()
		mangaSubscription = repository.observeCategories(mangaId)
			.map { list -> list.mapToSet { it.id } }
			.onEach { mangaCategories.postValue(it) }
			.launchIn(viewModelScope + Dispatchers.Default)
	}

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

	fun reorderCategories(oldPos: Int, newPos: Int) {
		val prevJob = reorderJob
		reorderJob = launchJob {
			prevJob?.join()
			val items = categories.value ?: error("This should not happen")
			val ids = items.mapTo(ArrayList(items.size)) { it.id }
			val item = ids.removeAt(oldPos)
			ids.add(newPos, item)
			repository.reorderCategories(ids)
		}
	}

	fun addToCategory(manga: Manga, categoryId: Long) {
		launchJob {
			repository.addToCategory(manga, categoryId)
		}
	}

	fun removeFromCategory(manga: Manga, categoryId: Long) {
		launchJob {
			repository.removeFromCategory(manga, categoryId)
		}
	}
}