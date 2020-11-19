package org.koitharu.kotatsu.favourites.ui.list

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toListDetailedModel
import org.koitharu.kotatsu.list.ui.model.toListModel

class FavouritesListViewModel(
	private val categoryId: Long,
	private val repository: FavouritesRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	override val content = combine(
		if (categoryId == 0L) repository.observeAll() else repository.observeAll(categoryId),
		createListModeFlow()
	) { list, mode ->
		when (mode) {
			ListMode.LIST -> list.map { it.toListModel() }
			ListMode.DETAILED_LIST -> list.map { it.toListDetailedModel() }
			ListMode.GRID -> list.map { it.toGridModel() }
		}
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	fun removeFromFavourites(manga: Manga) {
		launchJob {
			if (categoryId == 0L) {
				repository.removeFromFavourites(manga)
			} else {
				repository.removeFromCategory(manga, categoryId)
			}
		}
	}
}