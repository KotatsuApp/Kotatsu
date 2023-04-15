package org.koitharu.kotatsu.shelf.ui

import androidx.collection.ArraySet
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.EmptyHint
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.shelf.domain.ShelfContent
import org.koitharu.kotatsu.shelf.domain.ShelfRepository
import org.koitharu.kotatsu.shelf.domain.ShelfSection
import org.koitharu.kotatsu.shelf.ui.model.ShelfSectionModel
import org.koitharu.kotatsu.sync.domain.SyncController
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.asFlowLiveData
import javax.inject.Inject

@HiltViewModel
class ShelfViewModel @Inject constructor(
	private val repository: ShelfRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	syncController: SyncController,
	networkState: NetworkState,
) : BaseViewModel(), ListExtraProvider {

	val onActionDone = SingleLiveEvent<ReversibleAction>()

	val content: LiveData<List<ListModel>> = combine(
		settings.observeAsFlow(AppSettings.KEY_SHELF_SECTIONS) { shelfSections },
		settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled },
		networkState,
		repository.observeShelfContent(),
	) { sections, isTrackerEnabled, isConnected, content ->
		mapList(content, isTrackerEnabled, sections, isConnected)
	}.debounce(500)
		.catch { e ->
			emit(listOf(e.toErrorState(canRetry = false)))
		}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	init {
		launchJob(Dispatchers.Default) {
			syncController.requestFullSync()
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

	fun removeFromFavourites(category: FavouriteCategory, ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = favouritesRepository.removeFromCategory(category.id, ids)
			onActionDone.postCall(ReversibleAction(R.string.removed_from_favourites, handle))
		}
	}

	fun removeFromHistory(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = historyRepository.delete(ids)
			onActionDone.postCall(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	fun deleteLocal(ids: Set<Long>) {
		launchLoadingJob(Dispatchers.Default) {
			repository.deleteLocalManga(ids)
			onActionDone.postCall(ReversibleAction(R.string.removal_completed, null))
		}
	}

	fun clearHistory(minDate: Long) {
		launchJob(Dispatchers.Default) {
			val stringRes = if (minDate <= 0) {
				historyRepository.clear()
				R.string.history_cleared
			} else {
				historyRepository.deleteAfter(minDate)
				R.string.removed_from_history
			}
			onActionDone.postCall(ReversibleAction(stringRes, null))
		}
	}

	fun getManga(ids: Set<Long>): Set<Manga> {
		val snapshot = content.value ?: return emptySet()
		val result = ArraySet<Manga>(ids.size)
		for (section in snapshot) {
			if (section !is ShelfSectionModel) {
				continue
			}
			for (item in section.items) {
				if (item.id in ids) {
					result.add(item.manga)
					if (result.size == ids.size) {
						return result
					}
				}
			}
		}
		return result
	}

	private suspend fun mapList(
		content: ShelfContent,
		isTrackerEnabled: Boolean,
		sections: List<ShelfSection>,
		isNetworkAvailable: Boolean,
	): List<ListModel> {
		val result = ArrayList<ListModel>(content.favourites.keys.size + 3)
		if (isNetworkAvailable) {
			for (section in sections) {
				when (section) {
					ShelfSection.HISTORY -> mapHistory(result, content.history)
					ShelfSection.LOCAL -> mapLocal(result, content.local)
					ShelfSection.UPDATED -> if (isTrackerEnabled) {
						mapUpdated(result, content.updated)
					}

					ShelfSection.FAVORITES -> mapFavourites(result, content.favourites)
				}
			}
		} else {
			result += EmptyHint(
				icon = R.drawable.ic_empty_common,
				textPrimary = R.string.network_unavailable,
				textSecondary = R.string.network_unavailable_hint,
				actionStringRes = R.string.manage,
			)
			for (section in sections) {
				when (section) {
					ShelfSection.HISTORY -> mapHistory(
						result,
						content.history.filter { it.manga.source == MangaSource.LOCAL },
					)

					ShelfSection.LOCAL -> mapLocal(result, content.local)
					ShelfSection.UPDATED -> Unit
					ShelfSection.FAVORITES -> Unit
				}
			}
		}
		if (result.isEmpty()) {
			result += EmptyState(
				icon = R.drawable.ic_empty_history,
				textPrimary = R.string.text_shelf_holder_primary,
				textSecondary = R.string.text_shelf_holder_secondary,
				actionStringRes = 0,
			)
		} else {
			val one = result.singleOrNull()
			if (one is EmptyHint) {
				result[0] = one.toState()
			}
		}
		return result
	}

	private suspend fun mapHistory(
		destination: MutableList<in ShelfSectionModel.History>,
		list: List<MangaWithHistory>,
	) {
		if (list.isEmpty()) {
			return
		}
		val showPercent = settings.isReadingIndicatorsEnabled
		destination += ShelfSectionModel.History(
			items = list.map { (manga, history) ->
				val counter = getCounter(manga.id)
				val percent = if (showPercent) history.percent else PROGRESS_NONE
				manga.toGridModel(counter, percent)
			},
			showAllButtonText = R.string.show_all,
		)
	}

	private suspend fun mapUpdated(
		destination: MutableList<in ShelfSectionModel.Updated>,
		updated: Map<Manga, Int>,
	) {
		if (updated.isEmpty()) {
			return
		}
		val showPercent = settings.isReadingIndicatorsEnabled
		destination += ShelfSectionModel.Updated(
			items = updated.map { (manga, counter) ->
				val percent = if (showPercent) getProgress(manga.id) else PROGRESS_NONE
				manga.toGridModel(counter, percent)
			},
			showAllButtonText = R.string.show_all,
		)
	}

	private suspend fun mapLocal(
		destination: MutableList<in ShelfSectionModel.Local>,
		local: List<Manga>,
	) {
		if (local.isEmpty()) {
			return
		}
		destination += ShelfSectionModel.Local(
			items = local.toUi(ListMode.GRID, this, null),
			showAllButtonText = R.string.show_all,
		)
	}

	private suspend fun mapFavourites(
		destination: MutableList<in ShelfSectionModel.Favourites>,
		favourites: Map<FavouriteCategory, List<Manga>>,
	) {
		if (favourites.isEmpty()) {
			return
		}
		for ((category, list) in favourites) {
			if (list.isNotEmpty()) {
				destination += ShelfSectionModel.Favourites(
					items = list.toUi(ListMode.GRID, this, null),
					category = category,
					showAllButtonText = R.string.show_all,
				)
			}
		}
	}
}
