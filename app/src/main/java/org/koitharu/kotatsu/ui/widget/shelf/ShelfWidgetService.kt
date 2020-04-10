package org.koitharu.kotatsu.ui.widget.shelf

import android.content.Intent
import android.widget.RemoteViewsService

class ShelfWidgetService : RemoteViewsService() {

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return ShelfListFactory(this, intent)
	}
}