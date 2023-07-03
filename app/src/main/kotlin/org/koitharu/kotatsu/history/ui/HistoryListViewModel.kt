package org.koitharu.kotatsu.history.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.daysDiff
import org.koitharu.kotatsu.core.util.ext.onFirst
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.domain.model.MangaWithHistory
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toListDetailedModel
import org.koitharu.kotatsu.list.ui.model.toListModel
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HistoryListViewModel @Inject constructor(
	private val repository: HistoryRepository,
	private val settings: AppSettings,
	private val extraProvider: ListExtraProvider,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	val isGroupingEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_HISTORY_GROUPING,
		valueProducer = { isHistoryGroupingEnabled },
	)

	override val content = combine(
		repository.observeAllWithHistory(),
		isGroupingEnabled,
		listMode,
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
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory(minDate: Long) {
		launchJob(Dispatchers.Default) {
			val stringRes = if (minDate <= 0) {
				repository.clear()
				R.string.history_cleared
			} else {
				repository.deleteAfter(minDate)
				R.string.removed_from_history
			}
			onActionDone.call(ReversibleAction(stringRes, null))
		}
	}

	fun removeFromHistory(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = repository.delete(ids)
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
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
		var prevDate: DateTimeAgo? = null
		for ((manga, history) in list) {
			if (grouped) {
				val date = timeAgo(history.updatedAt)
				if (prevDate != date) {
					result += ListHeader(date, 0, null)
				}
				prevDate = date
			}
			result += when (mode) {
				ListMode.LIST -> manga.toListModel(extraProvider)
				ListMode.DETAILED_LIST -> manga.toListDetailedModel(extraProvider)
				ListMode.GRID -> manga.toGridModel(extraProvider)
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
			diffDays < 200 -> DateTimeAgo.MonthsAgo(diffDays / 30)
			else -> DateTimeAgo.LongAgo
		}
	}
}
