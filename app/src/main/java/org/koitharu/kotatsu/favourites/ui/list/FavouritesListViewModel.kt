package org.koitharu.kotatsu.favourites.ui.list

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment.Companion.NO_ID
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class FavouritesListViewModel(
	private val categoryId: Long,
	private val repository: FavouritesRepository,
	private val trackingRepository: TrackingRepository,
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings,
) : MangaListViewModel(settings), ListExtraProvider {

	private val sortOrder: StateFlow<SortOrder?> = if (categoryId == NO_ID) {
		MutableStateFlow(null)
	} else {
		repository.observeCategory(categoryId)
			.map { it?.order }
			.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)
	}

	override val content = combine(
		if (categoryId == NO_ID) {
			repository.observeAll(SortOrder.NEWEST)
		} else {
			repository.observeAll(categoryId)
		},
		sortOrder,
		createListModeFlow()
	) { list, order, mode ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_favourites,
					textPrimary = R.string.text_empty_holder_primary,
					textSecondary = if (categoryId == NO_ID) {
						R.string.you_have_not_favourites_yet
					} else {
						R.string.favourites_category_empty
					},
					actionStringRes = 0,
				)
			)
			else -> buildList<ListModel>(list.size + 1) {
				if (order != null) {
					add(ListHeader2(emptyList(), order, false))
				}
				list.toUi(this, mode, this@FavouritesListViewModel)
			}
		}
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun removeFromFavourites(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob {
			if (categoryId == NO_ID) {
				repository.removeFromFavourites(ids)
			} else {
				repository.removeFromCategory(categoryId, ids)
			}
		}
	}

	fun setSortOrder(order: SortOrder) {
		if (categoryId == NO_ID) {
			return
		}
		launchJob {
			repository.setCategoryOrder(categoryId, order)
		}
	}

	override suspend fun getCounter(mangaId: Long): Int {
		return trackingRepository.getNewChaptersCount(mangaId)
	}

	override suspend fun getProgress(mangaId: Long): Float {
		return if (settings.isReadingIndicatorsEnabled) {
			historyRepository.getProgress(mangaId)
		} else {
			PROGRESS_NONE
		}
	}
}