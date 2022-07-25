package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.mapItems
import org.koitharu.kotatsu.utils.ext.requireValue

@HiltViewModel
class FavouritesCategoriesViewModel @Inject constructor(
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var reorderJob: Job? = null
	private val isReorder = MutableStateFlow(false)

	val isInReorderMode = isReorder.asLiveData(viewModelScope.coroutineContext)

	val allCategories = repository.observeCategories()
		.mapItems {
			CategoryListModel(
				mangaCount = 0,
				covers = listOf(),
				category = it,
				isReorderMode = false,
			)
		}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val detalizedCategories = combine(
		repository.observeCategoriesWithCovers(),
		isReorder,
	) { list, reordering ->
		list.map { (category, covers) ->
			CategoryListModel(
				mangaCount = covers.size,
				covers = covers.take(3),
				category = category,
				isReorderMode = reordering,
			)
		}.ifEmpty {
			listOf(
				EmptyState(
					icon = R.drawable.ic_empty_favourites,
					textPrimary = R.string.text_empty_holder_primary,
					textSecondary = R.string.empty_favourite_categories,
					actionStringRes = 0,
				),
			)
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	fun deleteCategory(id: Long) {
		launchJob {
			repository.removeCategory(id)
		}
	}

	fun deleteCategories(ids: Set<Long>) {
		launchJob {
			repository.removeCategories(ids)
		}
	}

	fun setAllCategoriesVisible(isVisible: Boolean) {
		settings.isAllFavouritesVisible = isVisible
	}

	fun isInReorderMode(): Boolean = isReorder.value

	fun isEmpty(): Boolean = detalizedCategories.value?.none { it is CategoryListModel } ?: true

	fun setReorderMode(isReorderMode: Boolean) {
		isReorder.value = isReorderMode
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
