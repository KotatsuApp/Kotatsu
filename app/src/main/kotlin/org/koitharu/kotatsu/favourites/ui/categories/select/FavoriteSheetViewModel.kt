package org.koitharu.kotatsu.favourites.ui.categories.select

import androidx.collection.MutableLongObjectMap
import androidx.collection.MutableLongSet
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.ids
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.favourites.ui.categories.select.model.CategoriesHeaderItem
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.list.ui.model.ListModel
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
	private val header = CategoriesHeaderItem(
		titles = manga.map { it.title },
		covers = manga.take(3).map {
			Cover(
				url = it.coverUrl,
				source = it.source.name,
			)
		},
	)
	private val refreshTrigger = MutableStateFlow(Any())
	val content = combine(
		favouritesRepository.observeCategories(),
		refreshTrigger,
		settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled },
	) { categories, _, tracker ->
		mapList(categories, tracker)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(header))

	fun setChecked(categoryId: Long, isChecked: Boolean) {
		launchJob(Dispatchers.Default) {
			if (isChecked) {
				favouritesRepository.addToCategory(categoryId, manga)
			} else {
				favouritesRepository.removeFromCategory(categoryId, manga.ids())
			}
			refreshTrigger.value = Any()
		}
	}

	private suspend fun mapList(categories: List<FavouriteCategory>, tracker: Boolean): List<ListModel> {
		val cats = MutableLongObjectMap<MutableLongSet>(categories.size)
		categories.forEach { cats[it.id] = MutableLongSet(manga.size) }
		for (m in manga) {
			val ids = favouritesRepository.getCategoriesIds(m.id)
			ids.forEach { id -> cats[id]?.add(m.id) }
		}
		return buildList(categories.size + 1) {
			add(header)
			categories.mapTo(this) { cat ->
				MangaCategoryItem(
					category = cat,
					isChecked = cats[cat.id]?.isNotEmpty() == true,
					isTrackerEnabled = tracker,
					isEnabled = cats[cat.id]?.let { it.size == 0 || it.size == manga.size } == true,
				)
			}
		}
	}
}
