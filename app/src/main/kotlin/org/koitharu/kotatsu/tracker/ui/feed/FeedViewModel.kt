package org.koitharu.kotatsu.tracker.ui.feed

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.daysDiff
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.domain.model.TrackingLogItem
import org.koitharu.kotatsu.tracker.ui.feed.model.toFeedItem
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val PAGE_SIZE = 20

@HiltViewModel
class FeedViewModel @Inject constructor(
	private val repository: TrackingRepository,
) : BaseViewModel() {

	private val limit = MutableStateFlow(PAGE_SIZE)
	private val isReady = AtomicBoolean(false)

	val onFeedCleared = MutableEventFlow<Unit>()
	val content = repository.observeTrackingLog(limit)
		.map { list ->
			if (list.isEmpty()) {
				listOf(
					EmptyState(
						icon = R.drawable.ic_empty_feed,
						textPrimary = R.string.text_empty_holder_primary,
						textSecondary = R.string.text_feed_holder,
						actionStringRes = 0,
					),
				)
			} else {
				isReady.set(true)
				list.mapList()
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	fun clearFeed(clearCounters: Boolean) {
		launchLoadingJob(Dispatchers.Default) {
			repository.clearLogs()
			if (clearCounters) {
				repository.clearCounters()
			}
			onFeedCleared.call(Unit)
		}
	}

	fun requestMoreItems() {
		if (isReady.compareAndSet(true, false)) {
			limit.value += PAGE_SIZE
		}
	}

	private fun List<TrackingLogItem>.mapList(): List<ListModel> {
		val destination = ArrayList<ListModel>((size * 1.4).toInt())
		var prevDate: DateTimeAgo? = null
		for (item in this) {
			val date = timeAgo(item.createdAt)
			if (prevDate != date) {
				destination += ListHeader(date, 0, null)
			}
			prevDate = date
			destination += item.toFeedItem()
		}
		return destination
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
