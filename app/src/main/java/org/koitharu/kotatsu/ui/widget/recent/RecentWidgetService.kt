package org.koitharu.kotatsu.ui.widget.recent

import android.content.Intent
import android.widget.RemoteViewsService

class RecentWidgetService : RemoteViewsService() {

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return RecentListFactory(this, intent)
	}
}