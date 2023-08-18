package org.koitharu.kotatsu.favourites.ui.categories.select

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.model.ids
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteSheet.Companion.KEY_MANGA_LIST
import org.koitharu.kotatsu.favourites.ui.categories.select.model.CategoriesHeaderItem
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.list.ui.model.ListModel
import javax.inject.Inject

@HiltViewModel
class MangaCategoriesViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val favouritesRepository: FavouritesRepository,
	settings: AppSettings,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<List<ParcelableManga>>(KEY_MANGA_LIST).map { it.manga }
	private val header = CategoriesHeaderItem()

	val content: StateFlow<List<ListModel>> = combine(
		favouritesRepository.observeCategories(),
		observeCategoriesIds(),
	) { all, checked ->
		buildList(all.size + 1) {
			add(header)
			all.mapTo(this) {
				MangaCategoryItem(
					category = it,
					isChecked = it.id in checked,
					isTrackerEnabled = settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources,
				)
			}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

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
