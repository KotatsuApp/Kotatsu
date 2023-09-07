package org.koitharu.kotatsu.favourites.ui.categories

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.util.move
import javax.inject.Inject

@HiltViewModel
class FavouritesCategoriesViewModel @Inject constructor(
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var commitJob: Job? = null

	val content = MutableStateFlow<List<ListModel>>(listOf(LoadingState))

	init {
		launchJob(Dispatchers.Default) {
			repository.observeCategoriesWithCovers()
				.collectLatest {
					commitJob?.join()
					updateContent(it)
				}
		}
	}

	fun deleteCategories(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			repository.removeCategories(ids)
		}
	}

	fun setAllCategoriesVisible(isVisible: Boolean) {
		settings.isAllFavouritesVisible = isVisible
	}

	fun isEmpty(): Boolean = content.value.none { it is CategoryListModel }

	fun reorderCategories(oldPos: Int, newPos: Int) {
		val snapshot = content.requireValue().toMutableList()
		snapshot.move(oldPos, newPos)
		content.value = snapshot
		commit(snapshot)
	}

	fun setIsVisible(ids: Set<Long>, isVisible: Boolean) {
		launchJob(Dispatchers.Default) {
			for (id in ids) {
				repository.updateCategory(id, isVisible)
			}
		}
	}

	fun getCategories(ids: Set<Long>): ArrayList<FavouriteCategory> {
		val items = content.requireValue()
		return items.mapNotNullTo(ArrayList(ids.size)) { item ->
			(item as? CategoryListModel)?.category?.takeIf { it.id in ids }
		}
	}

	private fun commit(snapshot: List<ListModel>) {
		val prevJob = commitJob
		commitJob = launchJob {
			prevJob?.cancelAndJoin()
			delay(500)
			val ids = snapshot.mapNotNullTo(ArrayList(snapshot.size)) {
				(it as? CategoryListModel)?.category?.id
			}
			repository.reorderCategories(ids)
			yield()
		}
	}

	private fun updateContent(categories: Map<FavouriteCategory, List<Cover>>) {
		content.value = categories.map { (category, covers) ->
			CategoryListModel(
				mangaCount = covers.size,
				covers = covers.take(3),
				category = category,
				isTrackerEnabled = settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources,
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
	}
}
