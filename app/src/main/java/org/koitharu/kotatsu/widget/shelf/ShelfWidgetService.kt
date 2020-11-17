package org.koitharu.kotatsu.widget.shelf

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import org.koin.android.ext.android.get

class ShelfWidgetService : RemoteViewsService() {

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		val widgetId = intent.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID
		)
		return ShelfListFactory(this, get(), get(), widgetId)
	}
}