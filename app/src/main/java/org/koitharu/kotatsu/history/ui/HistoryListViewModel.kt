package org.koitharu.kotatsu.history.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.ReversibleHandle
import org.koitharu.kotatsu.base.domain.plus
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
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

	val isGroupingEnabled = MutableLiveData<Boolean>()
	val onItemsRemoved = SingleLiveEvent<ReversibleHandle>()

	private val historyGrouping = settings.observeAsFlow(AppSettings.KEY_HISTORY_GROUPING) { isHistoryGroupingEnabled }
		.onEach { isGroupingEnabled.postValue(it) }

	override val content = combine(
		repository.observeAllWithHistory(),
		historyGrouping,
		createListModeFlow()
	) { list, grouped, mode ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_history,
					textPrimary = R.string.text_history_holder_primary,
					textSecondary = R.string.text_history_holder_secondary,
					actionStringRes = 0,
				)
			)
			else -> mapList(list, grouped, mode)
		}
	}.onStart {
		loadingCounter.increment()
	}.onFirst {
		loadingCounter.decrement()
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

	fun removeFromHistory(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = repository.deleteReversible(ids) + ReversibleHandle {
				shortcutsRepository.updateShortcuts()
			}
			shortcutsRepository.updateShortcuts()
			onItemsRemoved.postCall(handle)
		}
	}

	fun setGrouping(isGroupingEnabled: Boolean) {
		settings.isHistoryGroupingEnabled = isGroupingEnabled
	}

	private suspend fun mapList(
		list: List<MangaWithHistory>,
		grouped: Boolean,
		mode: ListMode
	): List<ListModel> {
		val result = ArrayList<ListModel>(if (grouped) (list.size * 1.4).toInt() else list.size + 1)
		val showPercent = settings.isReadingIndicatorsEnabled
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
			val percent = if (showPercent) history.percent else PROGRESS_NONE
			result += when (mode) {
				ListMode.LIST -> manga.toListModel(counter, percent)
				ListMode.DETAILED_LIST -> manga.toListDetailedModel(counter, percent)
				ListMode.GRID -> manga.toGridModel(counter, percent)
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