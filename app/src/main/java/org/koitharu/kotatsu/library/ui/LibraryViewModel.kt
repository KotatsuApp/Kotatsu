package org.koitharu.kotatsu.library.ui

import androidx.collection.ArraySet
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
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
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.library.domain.LibraryRepository
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.daysDiff
import java.util.*

private const val HISTORY_MAX_SEGMENTS = 2

class LibraryViewModel(
	private val repository: LibraryRepository,
	private val historyRepository: HistoryRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
) : BaseViewModel(), ListExtraProvider {

	val onActionDone = SingleLiveEvent<ReversibleAction>()

	val content: LiveData<List<ListModel>> = combine(
		historyRepository.observeAllWithHistory(),
		repository.observeFavourites(SortOrder.NEWEST),
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

	private suspend fun mapList(
		history: List<MangaWithHistory>,
		favourites: Map<FavouriteCategory, List<Manga>>,
	): List<ListModel> {
		val result = ArrayList<ListModel>(favourites.keys.size + 1)
		if (history.isNotEmpty()) {
			result += mapHistory(history)
		}
		for ((category, list) in favourites) {
			result += LibrarySectionModel.Favourites(list.toUi(ListMode.GRID, this), category, R.string.show_all)
		}
		return result
	}

	private suspend fun mapHistory(list: List<MangaWithHistory>): List<LibrarySectionModel.History> {
		val showPercent = settings.isReadingIndicatorsEnabled
		val groups = ArrayList<DateTimeAgo>()
		val map = HashMap<DateTimeAgo, ArrayList<MangaItemModel>>()
		for ((manga, history) in list) {
			val date = timeAgo(history.updatedAt)
			val counter = trackingRepository.getNewChaptersCount(manga.id)
			val percent = if (showPercent) history.percent else PROGRESS_NONE
			if (groups.lastOrNull() != date) {
				groups.add(date)
			}
			map.getOrPut(date) { ArrayList() }.add(manga.toGridModel(counter, percent))
		}
		val result = ArrayList<LibrarySectionModel.History>(HISTORY_MAX_SEGMENTS)
		repeat(minOf(HISTORY_MAX_SEGMENTS - 1, groups.size - 1)) { i ->
			val key = groups[i]
			val values = map.remove(key)
			if (!values.isNullOrEmpty()) {
				result.add(LibrarySectionModel.History(values, key, 0))
			}
		}
		val values = map.values.flatten()
		if (values.isNotEmpty()) {
			val key = if (result.isEmpty()) {
				map.keys.singleOrNull()?.takeUnless { it == DateTimeAgo.LongAgo }
			} else {
				map.keys.singleOrNull() ?: DateTimeAgo.LongAgo
			}
			result.add(LibrarySectionModel.History(values, key, R.string.show_all))
		}
		return result
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
