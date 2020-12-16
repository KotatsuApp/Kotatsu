package org.koitharu.kotatsu.favourites.ui.list

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.utils.ext.asLiveData
import org.koitharu.kotatsu.utils.ext.onFirst

class FavouritesListViewModel(
	private val categoryId: Long,
	private val repository: FavouritesRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	override val content = combine(
		if (categoryId == 0L) repository.observeAll() else repository.observeAll(categoryId),
		createListModeFlow()
	) { list, mode ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					if (categoryId == 0L) {
						R.string.you_have_not_favourites_yet
					} else {
						R.string.favourites_category_empty
					}
				)
			)
			else -> list.toUi(mode)
		}
	}.onFirst {
		isLoading.postValue(false)
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.flowOn(Dispatchers.Default).asLiveData(viewModelScope.coroutineContext, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

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