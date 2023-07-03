package org.koitharu.kotatsu.favourites.ui.container

import dagger.hilt.android.lifecycle.HiltViewModel
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import javax.inject.Inject

@HiltViewModel
class FavouritesContainerViewModel @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val categories = favouritesRepository.observeCategories()
}
