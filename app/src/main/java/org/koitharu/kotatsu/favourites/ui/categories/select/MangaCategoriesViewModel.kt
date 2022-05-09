package org.koitharu.kotatsu.favourites.ui.categories.select

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.ids
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class MangaCategoriesViewModel(
	private val manga: List<Manga>,
	private val favouritesRepository: FavouritesRepository
) : BaseViewModel() {

	val content = combine(
		favouritesRepository.observeCategories(),
		observeCategoriesIds(),
	) { all, checked ->
		all.map {
			MangaCategoryItem(
				id = it.id,
				name = it.title,
				isChecked = it.id in checked
			)
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	fun setChecked(categoryId: Long, isChecked: Boolean) {
		launchJob(Dispatchers.Default) {
			if (isChecked) {
				favouritesRepository.addToCategory(categoryId, manga)
			} else {
				favouritesRepository.removeFromCategory(categoryId, manga.ids())
			}
		}
	}

	private fun observeCategoriesIds() = if (manga.size == 1) {
		// Fast path
		favouritesRepository.observeCategoriesIds(manga[0].id)
	} else {
		combine(
			manga.map { favouritesRepository.observeCategoriesIds(it.id) }
		) { array ->
			val result = HashSet<Long>()
			var isFirst = true
			for (ids in array) {
				if (isFirst) {
					result.addAll(ids)
					isFirst = false
				} else {
					result.retainAll(ids.toSet())
				}
			}
			result
		}
	}
}