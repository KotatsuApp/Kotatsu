package org.koitharu.kotatsu.ui.list.feed

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.presenterScope
import org.koin.core.component.inject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.domain.tracking.TrackingRepository
import org.koitharu.kotatsu.ui.base.BasePresenter

class FeedPresenter : BasePresenter<FeedView>() {

	private val repository by inject<TrackingRepository>()

	fun loadList(offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					repository.getTrackingLog(offset, 20)
				}
				if (offset == 0) {
					viewState.onListChanged(list)
				} else {
					viewState.onListAppended(list)
				}
			} catch (e: CancellationException) {
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				if (offset == 0) {
					viewState.onListError(e)
				} else {
					viewState.onError(e)
				}
			} finally {
				viewState.onLoadingStateChanged(false)
			}
		}
	}
}