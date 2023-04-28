package org.koitharu.kotatsu.history.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.parser.MangaTagHighlighter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toListDetailedModel
import org.koitharu.kotatsu.list.ui.model.toListModel
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.asFlowLiveData
import org.koitharu.kotatsu.utils.ext.daysDiff
import org.koitharu.kotatsu.utils.ext.emitValue
import org.koitharu.kotatsu.utils.ext.onFirst
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HistoryListViewModel @Inject constructor(
	private val repository: HistoryRepository,
	private val settings: AppSettings,
	private val trackingRepository: TrackingRepository,
	private val tagHighlighter: MangaTagHighlighter,
) : MangaListViewModel(settings) {

	val isGroupingEnabled = MutableLiveData<Boolean>()

	private val historyGrouping = settings.observeAsFlow(AppSettings.KEY_HISTORY_GROUPING) { isHistoryGroupingEnabled }
		.onEach { isGroupingEnabled.emitValue(it) }

	override val content = combine(
		repository.observeAllWithHistory(),
		historyGrouping,
		listModeFlow,
	) { list, grouped, mode ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_history,
					textPrimary = R.string.text_history_holder_primary,
					textSecondary = R.string.text_history_holder_secondary,
					actionStringRes = 0,
				),
			)

			else -> mapList(list, grouped, mode)
		}
	}.onStart {
		loadingCounter.increment()
	}.onFirst {
		loadingCounter.decrement()
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory() {
		launchLoadingJob(Dispatchers.Default) {
			repository.clear()
		}
	}

	fun removeFromHistory(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = repository.delete(ids)
			onActionDone.emitCall(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	fun setGrouping(isGroupingEnabled: Boolean) {
		settings.isHistoryGroupingEnabled = isGroupingEnabled
	}

	private suspend fun mapList(
		list: List<MangaWithHistory>,
		grouped: Boolean,
		mode: ListMode,
	): List<ListModel> {
		val result = ArrayList<ListModel>(if (grouped) (list.size * 1.4).toInt() else list.size + 1)
		val showPercent = settings.isReadingIndicatorsEnabled
		var prevDate: DateTimeAgo? = null
		for ((manga, history) in list) {
			if (grouped) {
				val date = timeAgo(history.updatedAt)
				if (prevDate != date) {
					result += date
				}
				prevDate = date
			}
			val counter = if (settings.isTrackerEnabled) {
				trackingRepository.getNewChaptersCount(manga.id)
			} else {
				0
			}
			val percent = if (showPercent) history.percent else PROGRESS_NONE
			result += when (mode) {
				ListMode.LIST -> manga.toListModel(counter, percent)
				ListMode.DETAILED_LIST -> manga.toListDetailedModel(counter, percent, tagHighlighter)
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
