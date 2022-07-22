package org.koitharu.kotatsu.library.ui

import androidx.collection.ArraySet
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.library.domain.LibraryRepository
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.daysDiff

private const val HISTORY_MAX_SEGMENTS = 2

class LibraryViewModel(
	repository: LibraryRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
) : BaseViewModel(), ListExtraProvider {

	val onActionDone = SingleLiveEvent<ReversibleAction>()

	val content: LiveData<List<ListModel>> = combine(
		historyRepository.observeAllWithHistory(),
		repository.observeFavourites(),
	) { history, favourites ->
		mapList(history, favourites)
	}.catch { e ->
		e.toErrorState(canRetry = false)
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

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
			if (section !is LibrarySectionModel) {
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
		history: List<MangaWithHistory>,
		favourites: Map<FavouriteCategory, List<Manga>>,
	): List<ListModel> {
		val result = ArrayList<ListModel>(favourites.keys.size + 1)
		if (history.isNotEmpty()) {
			mapHistory(result, history)
		}
		if (favourites.isNotEmpty()) {
			mapFavourites(result, favourites)
		}
		if (result.isEmpty()) {
			result += EmptyState(
				icon = R.drawable.ic_empty_history,
				textPrimary = R.string.text_history_holder_primary,
				textSecondary = R.string.text_history_holder_secondary,
				actionStringRes = 0,
			)
		}
		result.trimToSize()
		return result
	}

	private suspend fun mapHistory(
		destination: MutableList<in LibrarySectionModel.History>,
		list: List<MangaWithHistory>,
	) {
		val showPercent = settings.isReadingIndicatorsEnabled
		val groups = list.groupByTo(LinkedHashMap()) { timeAgo(it.history.updatedAt) }
		while (groups.size > HISTORY_MAX_SEGMENTS) {
			val lastKey = groups.keys.last()
			val subList = groups.remove(lastKey) ?: continue
			groups[groups.keys.last()]?.addAll(subList)
		}
		for ((timeAgo, subList) in groups) {
			destination += LibrarySectionModel.History(
				items = subList.map { (manga, history) ->
					val counter = trackingRepository.getNewChaptersCount(manga.id)
					val percent = if (showPercent) history.percent else PROGRESS_NONE
					manga.toGridModel(counter, percent)
				},
				timeAgo = timeAgo,
				showAllButtonText = R.string.show_all,
			)
		}
	}

	private suspend fun mapFavourites(
		destination: MutableList<in LibrarySectionModel.Favourites>,
		favourites: Map<FavouriteCategory, List<Manga>>,
	) {
		for ((category, list) in favourites) {
			destination += LibrarySectionModel.Favourites(
				items = list.toUi(ListMode.GRID, this),
				category = category,
				showAllButtonText = R.string.show_all,
			)
		}
	}

	private fun timeAgo(date: Date): DateTimeAgo {
		val diffDays = -date.daysDiff(System.currentTimeMillis())
		return when {
			diffDays < 1 -> DateTimeAgo.Today
			diffDays == 1 -> DateTimeAgo.Yesterday
			diffDays <= 3 -> DateTimeAgo.DaysAgo(diffDays)
			else -> DateTimeAgo.LongAgo
		}
	}
}
