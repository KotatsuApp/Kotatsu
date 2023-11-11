package org.koitharu.kotatsu.favourites.ui.categories.select

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.model.ids
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.firstNotNull
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.select.model.CategoriesHeaderItem
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.parsers.util.mapToSet
import javax.inject.Inject

@HiltViewModel
class FavoriteSheetViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val favouritesRepository: FavouritesRepository,
	settings: AppSettings,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<List<ParcelableManga>>(FavoriteSheet.KEY_MANGA_LIST).mapToSet {
		it.manga
	}
	private val header = CategoriesHeaderItem()
	private val checkedCategories = MutableStateFlow<Set<Long>?>(null)
	val content = combine(
		favouritesRepository.observeCategories(),
		checkedCategories.filterNotNull(),
		settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled },
	) { categories, checked, tracker ->
		buildList(categories.size + 1) {
			add(header)
			categories.mapTo(this) { cat ->
				MangaCategoryItem(
					category = cat,
					isChecked = cat.id in checked,
					isTrackerEnabled = tracker,
				)
			}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(header))

	init {
		launchJob(Dispatchers.Default) {
			checkedCategories.value = favouritesRepository.getCategoriesIds(manga.ids())
		}
	}

	fun setChecked(categoryId: Long, isChecked: Boolean) {
		launchJob(Dispatchers.Default) {
			val checkedIds = checkedCategories.firstNotNull()
			if (isChecked) {
				checkedCategories.value = checkedIds + categoryId
				favouritesRepository.addToCategory(categoryId, manga)
			} else {
				checkedCategories.value = checkedIds - categoryId
				favouritesRepository.removeFromCategory(categoryId, manga.ids())
			}
		}
	}
}
