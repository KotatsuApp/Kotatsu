package org.koitharu.kotatsu.library.ui.config.categories

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class LibraryCategoriesConfigViewModel(
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val content = favouritesRepository.observeCategories()
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	private var updateJob: Job? = null

	fun toggleItem(category: FavouriteCategory) {
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.Default) {
			prevJob?.join()
			favouritesRepository.updateCategory(category.id, !category.isVisibleInLibrary)
		}
	}
}
