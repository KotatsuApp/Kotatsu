package org.koitharu.kotatsu.tracker.ui

import androidx.lifecycle.MutableLiveData
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.tracker.domain.TrackingRepository

class FeedViewModel(
	private val repository: TrackingRepository
) : BaseViewModel() {

	val content = MutableLiveData<List<TrackingLogItem>>()

	fun loadList(offset: Int) {
		launchLoadingJob {
			val list = repository.getTrackingLog(offset, 20)
			if (offset == 0) {
				content.value = list
			} else {
				content.value = content.value.orEmpty() + list
			}
		}
	}
}