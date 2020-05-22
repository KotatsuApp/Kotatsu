package org.koitharu.kotatsu.ui.main.tracklogs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.domain.tracking.TrackingRepository
import org.koitharu.kotatsu.ui.common.BasePresenter

class FeedPresenter : BasePresenter<FeedView>() {

	private lateinit var repository: TrackingRepository

	override fun onFirstViewAttach() {
		repository = TrackingRepository()
		super.onFirstViewAttach()
	}

	fun loadList(offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					repository.getTrackingLog(offset, 20)
				}
				viewState.onListChanged(list)
			} catch (e: CancellationException) {
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onListError(e)
			} finally {
				viewState.onLoadingStateChanged(false)
			}
		}
	}
}