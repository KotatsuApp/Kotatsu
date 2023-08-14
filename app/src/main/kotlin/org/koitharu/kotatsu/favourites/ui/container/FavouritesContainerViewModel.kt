package org.koitharu.kotatsu.favourites.ui.container

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.mapItems
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import javax.inject.Inject

@HiltViewModel
class FavouritesContainerViewModel @Inject constructor(
	favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	private val categoriesStateFlow = favouritesRepository.observeCategoriesForLibrary()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val categories = categoriesStateFlow.filterNotNull()
		.mapItems { FavouriteTabModel(it.id, it.title) }
		.distinctUntilChanged()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val isEmpty = categoriesStateFlow.map {
		it?.isEmpty() == true
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)
}
