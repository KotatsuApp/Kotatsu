package org.koitharu.kotatsu.favourites.ui.categories.select

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.ids
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet.Companion.KEY_MANGA_LIST
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class MangaCategoriesViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	private val manga = requireNotNull(savedStateHandle.get<List<ParcelableManga>>(KEY_MANGA_LIST)).map { it.manga }

	val content = combine(
		favouritesRepository.observeCategories(),
		observeCategoriesIds(),
	) { all, checked ->
		all.map {
			MangaCategoryItem(
				id = it.id,
				name = it.title,
				isChecked = it.id in checked,
			)
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

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
			manga.map { favouritesRepository.observeCategoriesIds(it.id) },
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
