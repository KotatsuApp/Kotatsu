package org.koitharu.kotatsu.favourites.ui.categories.select

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem

class MangaCategoriesViewModel(
	private val manga: Manga,
	private val favouritesRepository: FavouritesRepository
) : BaseViewModel() {

	val content = combine(
		favouritesRepository.observeCategories(),
		favouritesRepository.observeCategoriesIds(manga.id)
	) { all, checked ->
		all.map {
			MangaCategoryItem(
				id = it.id,
				name = it.title,
				isChecked = it.id in checked
			)
		}
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	fun setChecked(categoryId: Long, isChecked: Boolean) {
		launchJob(Dispatchers.Default) {
			if (isChecked) {
				favouritesRepository.addToCategory(manga, categoryId)
			} else {
				favouritesRepository.removeFromCategory(manga, categoryId)
			}
		}
	}

	fun createCategory(name: String) {
		launchJob(Dispatchers.Default) {
			favouritesRepository.addCategory(name)
		}
	}
}