package org.koitharu.kotatsu.library.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.library.ui.model.LibraryGroupModel
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.daysDiff
import java.util.*

private const val HISTORY_MAX_SEGMENTS = 2

class LibraryViewModel(
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val shortcutsRepository: ShortcutsRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
) : BaseViewModel(), ListExtraProvider {

	val content: LiveData<List<ListModel>> = combine(
		historyRepository.observeAllWithHistory(),
		favouritesRepository.observeAllGrouped(SortOrder.NEWEST),
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

	private suspend fun mapList(
		history: List<MangaWithHistory>,
		favourites: Map<FavouriteCategory, List<Manga>>,
	): List<ListModel> {
		val result = ArrayList<ListModel>(favourites.keys.size + 1)
		if (history.isNotEmpty()) {
			result += mapHistory(history)
		}
		for ((category, list) in favourites) {
			result += LibraryGroupModel.Favourites(list.toUi(ListMode.GRID, this), category, R.string.show_all)
		}
		return result
	}

	private suspend fun mapHistory(list: List<MangaWithHistory>): List<LibraryGroupModel.History> {
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
		val result = ArrayList<LibraryGroupModel.History>(HISTORY_MAX_SEGMENTS)
		repeat(minOf(HISTORY_MAX_SEGMENTS - 1, groups.size - 1)) { i ->
			val key = groups[i]
			val values = map.remove(key)
			if (!values.isNullOrEmpty()) {
				result.add(LibraryGroupModel.History(values, key, 0))
			}
		}
		val values = map.values.flatten()
		if (values.isNotEmpty()) {
			val key = if (result.isEmpty()) {
				map.keys.singleOrNull()?.takeUnless { it == DateTimeAgo.LongAgo }
			} else {
				map.keys.singleOrNull() ?: DateTimeAgo.LongAgo
			}
			result.add(LibraryGroupModel.History(values, key, R.string.show_all))
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