package org.koitharu.kotatsu.tracker.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.list.ui.model.IndeterminateProgress
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.ui.model.toFeedItem
import org.koitharu.kotatsu.utils.ext.mapItems

class FeedViewModel(
	context: Context,
	private val repository: TrackingRepository
) : BaseViewModel() {

	private val logList = MutableStateFlow<List<TrackingLogItem>>(emptyList())
	private val hasNextPage = MutableStateFlow(false)
	private var loadingJob: Job? = null

	val isEmptyState = MutableLiveData(false)
	val content = combine(
		logList.drop(1).mapItems {
			it.toFeedItem(context.resources)
		},
		hasNextPage
	) { list, isHasNextPage ->
		if (isHasNextPage && list.isNotEmpty()) list + IndeterminateProgress else list
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		loadList(append = false)
	}

	fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		loadingJob = launchLoadingJob {
			val offset = if (append) logList.value.size else 0
			val list = repository.getTrackingLog(offset, 20)
			if (!append) {
				logList.value = list
			} else if (list.isNotEmpty()) {
				logList.value += list
			} else {
				isEmptyState.value = true
			}
			hasNextPage.value = list.isNotEmpty()
		}
	}
}