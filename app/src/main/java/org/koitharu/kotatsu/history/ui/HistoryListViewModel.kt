package org.koitharu.kotatsu.history.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.daysDiff
import org.koitharu.kotatsu.utils.ext.onFirst
import java.util.*
import java.util.concurrent.TimeUnit

class HistoryListViewModel(
	private val repository: HistoryRepository,
	private val settings: AppSettings,
	private val shortcutsRepository: ShortcutsRepository,
	private val trackingRepository: TrackingRepository,
) : MangaListViewModel(settings) {

	val onItemRemoved = SingleLiveEvent<Manga>()
	val isGroupingEnabled = MutableLiveData<Boolean>()

	private val historyGrouping = settings.observe()
		.filter { it == AppSettings.KEY_HISTORY_GROUPING }
		.map { settings.historyGrouping }
		.onStart { emit(settings.historyGrouping) }
		.distinctUntilChanged()
		.onEach { isGroupingEnabled.postValue(it) }

	override val content = combine(
		repository.observeAllWithHistory(),
		historyGrouping,
		createListModeFlow()
	) { list, grouped, mode ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_history,
					textPrimary = R.string.text_history_holder_primary,
					textSecondary = R.string.text_history_holder_secondary,
					actionStringRes = 0,
				)
			)
			else -> mapList(list, grouped, mode)
		}
	}.onFirst {
		isLoading.postValue(false)
	}.catch {
		it.toErrorState(canRetry = false)
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory() {
		launchLoadingJob {
			repository.clear()
			shortcutsRepository.updateShortcuts()
		}
	}

	fun removeFromHistory(manga: Manga) {
		launchJob {
			repository.delete(manga)
			onItemRemoved.call(manga)
			shortcutsRepository.updateShortcuts()
		}
	}

	fun setGrouping(isGroupingEnabled: Boolean) {
		settings.historyGrouping = isGroupingEnabled
	}

	private suspend fun mapList(
		list: List<MangaWithHistory>,
		grouped: Boolean,
		mode: ListMode
	): List<ListModel> {
		val result = ArrayList<ListModel>(if (grouped) (list.size * 1.4).toInt() else list.size + 1)
		var prevDate: DateTimeAgo? = null
		if (!grouped) {
			result += ListHeader(null, R.string.history, null)
		}
		for ((manga, history) in list) {
			if (grouped) {
				val date = timeAgo(history.updatedAt)
				if (prevDate != date) {
					result += date
				}
				prevDate = date
			}
			val counter = trackingRepository.getNewChaptersCount(manga.id)
			result += when (mode) {
				ListMode.LIST -> manga.toListModel(counter)
				ListMode.DETAILED_LIST -> manga.toListDetailedModel(counter)
				ListMode.GRID -> manga.toGridModel(counter)
			}
		}
		return result
	}

	private fun timeAgo(date: Date): DateTimeAgo {
		val diff = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
		val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
		val diffDays = -date.daysDiff(System.currentTimeMillis())
		return when {
			diffMinutes < 3 -> DateTimeAgo.JustNow
			diffDays < 1 -> DateTimeAgo.Today
			diffDays == 1 -> DateTimeAgo.Yesterday
			diffDays < 6 -> DateTimeAgo.DaysAgo(diffDays)
			else -> DateTimeAgo.LongAgo
		}
	}
}