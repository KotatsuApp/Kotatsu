package org.koitharu.kotatsu.widget.recent

import android.content.Intent
import android.widget.RemoteViewsService
import org.koin.android.ext.android.get

class RecentWidgetService : RemoteViewsService() {

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return RecentListFactory(applicationContext, get(), get())
	}
}