package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.mapItems
import org.koitharu.kotatsu.utils.ext.requireValue
import java.util.*

class FavouritesCategoriesViewModel(
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var reorderJob: Job? = null

	val allCategories = repository.observeCategories()
		.mapItems {
			CategoryListModel(
				mangaCount = 0,
				covers = listOf(),
				category = it,
			)
		}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val detalizedCategories = repository.observeCategoriesWithDetails()
		.map {
			it.map { (category, covers) ->
				CategoryListModel(
					mangaCount = covers.size,
					covers = covers.take(3),
					category = category,
				)
			}
		}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	fun deleteCategory(id: Long) {
		launchJob {
			repository.removeCategory(id)
		}
	}

	fun setAllCategoriesVisible(isVisible: Boolean) {
		settings.isAllFavouritesVisible = isVisible
	}

	fun reorderCategories(oldPos: Int, newPos: Int) {
		val prevJob = reorderJob
		reorderJob = launchJob(Dispatchers.Default) {
			prevJob?.join()
			val items = detalizedCategories.requireValue()
			val ids = items.mapNotNullTo(ArrayList(items.size)) {
				(it as? CategoryListModel)?.category?.id
			}
			Collections.swap(ids, oldPos, newPos)
			ids.remove(0L)
			repository.reorderCategories(ids)
		}
	}
}