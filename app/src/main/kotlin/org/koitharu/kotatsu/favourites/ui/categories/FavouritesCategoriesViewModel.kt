package org.koitharu.kotatsu.favourites.ui.categories

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.LoadingState
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class FavouritesCategoriesViewModel @Inject constructor(
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var reorderJob: Job? = null

	val categories = repository.observeCategoriesWithCovers()
		.map { list ->
			list.map { (category, covers) ->
				CategoryListModel(
					mangaCount = covers.size,
					covers = covers.take(3),
					category = category,
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
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	fun deleteCategories(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			repository.removeCategories(ids)
		}
	}

	fun setAllCategoriesVisible(isVisible: Boolean) {
		settings.isAllFavouritesVisible = isVisible
	}

	fun isEmpty(): Boolean = categories.value.none { it is CategoryListModel }

	fun reorderCategories(oldPos: Int, newPos: Int) {
		val prevJob = reorderJob
		reorderJob = launchJob(Dispatchers.Default) {
			prevJob?.join()
			val items = categories.requireValue()
			val ids = items.mapNotNullTo(ArrayList(items.size)) {
				(it as? CategoryListModel)?.category?.id
			}
			Collections.swap(ids, oldPos, newPos)
			ids.remove(0L)
			repository.reorderCategories(ids)
		}
	}

	fun setIsVisible(ids: Set<Long>, isVisible: Boolean) {
		launchJob(Dispatchers.Default) {
			for (id in ids) {
				repository.updateCategory(id, isVisible)
			}
		}
	}

	fun getCategories(ids: Set<Long>): ArrayList<FavouriteCategory> {
		val items = categories.requireValue()
		return items.mapNotNullTo(ArrayList(ids.size)) { item ->
			(item as? CategoryListModel)?.category?.takeIf { it.id in ids }
		}
	}
}
