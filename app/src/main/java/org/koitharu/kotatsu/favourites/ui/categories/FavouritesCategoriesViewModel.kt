package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import java.util.*

class FavouritesCategoriesViewModel(
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var reorderJob: Job? = null

	val allCategories = combine(
		repository.observeCategories(),
		observeAllCategoriesVisible(),
	) { list, showAll ->
		mapCategories(list, showAll, true)
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)
	
	val visibleCategories = combine(
		repository.observeCategories(),
		observeAllCategoriesVisible(),
	) { list, showAll ->
		mapCategories(list, showAll, showAll && list.isNotEmpty())
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

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
			val items = allCategories.value ?: error("This should not happen")
			val ids = items.mapTo(ArrayList(items.size)) { it.id }
			Collections.swap(ids, oldPos, newPos)
			ids.remove(0L)
			repository.reorderCategories(ids)
		}
	}

	private fun mapCategories(
		categories: List<FavouriteCategory>,
		isAllCategoriesVisible: Boolean,
		withAllCategoriesItem: Boolean,
	): List<CategoryListModel> {
		val result = ArrayList<CategoryListModel>(categories.size + 1)
		if (withAllCategoriesItem) {
			result.add(CategoryListModel.All(isAllCategoriesVisible))
		}
		categories.mapTo(result) {
			CategoryListModel.CategoryItem(it)
		}
		return result
	}

	private fun observeAllCategoriesVisible() = settings.observe()
		.filter { it == AppSettings.KEY_ALL_FAVOURITES_VISIBLE }
		.map { settings.isAllFavouritesVisible }
		.onStart { emit(settings.isAllFavouritesVisible) }
		.distinctUntilChanged()
}