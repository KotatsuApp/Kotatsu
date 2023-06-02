package org.koitharu.kotatsu.favourites.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaTagHighlighter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment.Companion.ARG_CATEGORY_ID
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment.Companion.NO_ID
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import javax.inject.Inject

@HiltViewModel
class FavouritesListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val repository: FavouritesRepository,
	private val trackingRepository: TrackingRepository,
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings,
	private val tagHighlighter: MangaTagHighlighter,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler), ListExtraProvider {

	val categoryId: Long = savedStateHandle[ARG_CATEGORY_ID] ?: NO_ID

	val sortOrder: StateFlow<SortOrder?> = if (categoryId == NO_ID) {
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
		listMode,
	) { list, mode ->
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
				),
			)

			else -> list.toUi(mode, this, tagHighlighter)
		}
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun removeFromFavourites(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = if (categoryId == NO_ID) {
				repository.removeFromFavourites(ids)
			} else {
				repository.removeFromCategory(categoryId, ids)
			}
			onActionDone.call(ReversibleAction(R.string.removed_from_favourites, handle))
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
		return if (settings.isTrackerEnabled) {
			trackingRepository.getNewChaptersCount(mangaId)
		} else {
			0
		}
	}

	override suspend fun getProgress(mangaId: Long): Float {
		return if (settings.isReadingIndicatorsEnabled) {
			historyRepository.getProgress(mangaId)
		} else {
			PROGRESS_NONE
		}
	}
}