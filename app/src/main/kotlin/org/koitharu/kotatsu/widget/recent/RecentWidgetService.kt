package org.koitharu.kotatsu.widget.recent

import android.content.Intent
import android.widget.RemoteViewsService
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.history.data.HistoryRepository
import javax.inject.Inject

@AndroidEntryPoint
class RecentWidgetService : RemoteViewsService() {

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var coil: ImageLoader

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return RecentListFactory(applicationContext, historyRepository, coil)
	}
}
