package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.utils.MangaShortcut
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.daysDiff
import org.koitharu.kotatsu.utils.ext.onFirst
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class HistoryListViewModel(
	private val repository: HistoryRepository,
	private val context: Context, //todo create ShortcutRepository
	private val settings: AppSettings
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
			list.isEmpty() -> listOf(EmptyState(R.string.text_history_holder))
			else -> mapList(list, grouped, mode)
		}
	}.onFirst {
		isLoading.postValue(false)
	}.onStart {
		emit(listOf(LoadingState))
	}.catch {
		it.toErrorState(canRetry = false)
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory() {
		launchLoadingJob {
			repository.clear()
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut.clearAppShortcuts(context)
			}
		}
	}

	fun removeFromHistory(manga: Manga) {
		launchJob {
			repository.delete(manga)
			onItemRemoved.call(manga)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut(manga).removeAppShortcut(context)
			}
		}
	}

	fun setGrouping(isGroupingEnabled: Boolean) {
		settings.historyGrouping = isGroupingEnabled
	}

	private fun mapList(list: List<MangaWithHistory>, grouped: Boolean, mode: ListMode): List<ListModel> {
		val result = ArrayList<ListModel>(if (grouped) (list.size * 1.4).toInt() else list.size)
		var prevDate: DateTimeAgo? = null
		for ((manga, history) in list) {
			if (grouped) {
				val date = timeAgo(history.updatedAt)
				if (prevDate != date) {
					result += date
				}
				prevDate = date
			}
			result += when (mode) {
				ListMode.LIST -> manga.toListModel()
				ListMode.DETAILED_LIST -> manga.toListDetailedModel()
				ListMode.GRID -> manga.toGridModel()
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