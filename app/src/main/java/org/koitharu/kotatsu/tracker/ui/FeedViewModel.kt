package org.koitharu.kotatsu.tracker.ui

import androidx.lifecycle.viewModelScope
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.domain.model.TrackingLogItem
import org.koitharu.kotatsu.tracker.ui.model.toFeedItem
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.daysDiff

class FeedViewModel(
	private val repository: TrackingRepository,
) : BaseViewModel() {

	private val logList = MutableStateFlow<List<TrackingLogItem>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private var loadingJob: Job? = null
	private val header = ListHeader(null, R.string.updates, null)

	val onFeedCleared = SingleLiveEvent<Unit>()
	val content = combine(
		logList.filterNotNull(),
		hasNextPage,
	) { list, isHasNextPage ->
		buildList(list.size + 2) {
			if (list.isEmpty()) {
				add(header)
				add(
					EmptyState(
						icon = R.drawable.ic_empty_feed,
						textPrimary = R.string.text_empty_holder_primary,
						textSecondary = R.string.text_feed_holder,
						actionStringRes = 0,
					),
				)
			} else {
				list.mapListTo(this)
				if (isHasNextPage) {
					add(LoadingFooter)
				}
			}
		}
	}.asLiveDataDistinct(
		viewModelScope.coroutineContext + Dispatchers.Default,
		listOf(header, LoadingState),
	)

	init {
		loadList(append = false)
	}

	fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		if (append && !hasNextPage.value) {
			return
		}
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			val offset = if (append) logList.value?.size ?: 0 else 0
			val list = repository.getTrackingLog(offset, 20)
			if (!append) {
				logList.value = list
			} else if (list.isNotEmpty()) {
				logList.value = logList.value?.plus(list) ?: list
			}
			hasNextPage.value = list.isNotEmpty()
		}
	}

	fun clearFeed() {
		val lastJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			lastJob?.cancelAndJoin()
			repository.clearLogs()
			logList.value = emptyList()
			onFeedCleared.postCall(Unit)
		}
	}

	private fun List<TrackingLogItem>.mapListTo(destination: MutableList<ListModel>) {
		var prevDate: DateTimeAgo? = null
		for (item in this) {
			val date = timeAgo(item.createdAt)
			if (prevDate != date) {
				destination += date
			}
			prevDate = date
			destination += item.toFeedItem()
		}
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
			else -> DateTimeAgo.Absolute(date)
		}
	}
}