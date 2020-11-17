package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.utils.ext.mapToSet

class FavouritesCategoriesViewModel(
	private val repository: FavouritesRepository
) : BaseViewModel() {

	private var reorderJob: Job? = null

	val categories = MutableLiveData<List<FavouriteCategory>>()
	val mangaCategories = MutableLiveData<Set<Int>>()

	init {
		loadAllCategories()
	}

	fun loadAllCategories() {
		launchJob {
			categories.value = repository.getAllCategories()
		}
	}

	fun loadMangaCategories(manga: Manga) {
		launchJob {
			val categories = repository.getCategories(manga.id)
			mangaCategories.value = categories.mapToSet { it.id.toInt() }
		}
	}

	fun createCategory(name: String) {
		launchJob {
			repository.addCategory(name)
			categories.value = repository.getAllCategories()
		}
	}

	fun renameCategory(id: Long, name: String) {
		launchJob {
			repository.renameCategory(id, name)
			categories.value = repository.getAllCategories()
		}
	}

	fun deleteCategory(id: Long) {
		launchJob {
			repository.removeCategory(id)
			categories.value = repository.getAllCategories()
		}
	}

	fun storeCategoriesOrder(orderedIds: List<Long>) {
		val prevJob = reorderJob
		reorderJob = launchJob {
			prevJob?.join()
			repository.reorderCategories(orderedIds)
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