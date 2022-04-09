package org.koitharu.kotatsu.tracker.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.ui.model.toFeedItem
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class FeedViewModel(
	private val repository: TrackingRepository
) : BaseViewModel() {

	private val logList = MutableStateFlow<List<TrackingLogItem>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private var loadingJob: Job? = null
	private val header = ListHeader(null, R.string.updates, null)

	val onFeedCleared = SingleLiveEvent<Unit>()
	val content = combine(
		logList.filterNotNull(),
		hasNextPage
	) { list, isHasNextPage ->
		buildList(list.size + 2) {
			add(header)
			if (list.isEmpty()) {
				add(
					EmptyState(
						icon = R.drawable.ic_feed,
						textPrimary = R.string.text_empty_holder_primary,
						textSecondary = R.string.text_feed_holder,
						actionStringRes = 0,
					)
				)
			} else {
				list.mapTo(this) { it.toFeedItem() }
				if (isHasNextPage) {
					add(LoadingFooter)
				}
			}
		}
	}.asLiveDataDistinct(
		viewModelScope.coroutineContext + Dispatchers.Default,
		listOf(header, LoadingState)
	)

	init {
		loadList(append = false)
	}

	fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
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
}