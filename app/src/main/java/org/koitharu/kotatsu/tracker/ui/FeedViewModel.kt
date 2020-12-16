package org.koitharu.kotatsu.tracker.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.ui.model.toFeedItem
import org.koitharu.kotatsu.utils.ext.asLiveData
import org.koitharu.kotatsu.utils.ext.mapItems

class FeedViewModel(
	context: Context,
	private val repository: TrackingRepository
) : BaseViewModel() {

	private val logList = MutableStateFlow<List<TrackingLogItem>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private var loadingJob: Job? = null

	val isEmptyState = MutableLiveData(false)
	val content = combine(
		logList.filterNotNull().mapItems {
			it.toFeedItem(context.resources)
		},
		hasNextPage
	) { list, isHasNextPage ->
		if (isHasNextPage && list.isNotEmpty()) list + LoadingFooter else list
	}.flowOn(Dispatchers.Default).asLiveData(viewModelScope.coroutineContext, listOf(LoadingState))

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
				isEmptyState.postValue(list.isEmpty())
			} else if (list.isNotEmpty()) {
				logList.value = logList.value?.plus(list) ?: list
			}
			hasNextPage.value = list.isNotEmpty()
		}
	}
}