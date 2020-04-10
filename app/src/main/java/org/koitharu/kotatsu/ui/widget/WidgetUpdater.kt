package org.koitharu.kotatsu.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.koitharu.kotatsu.domain.favourites.OnFavouritesChangeListener
import org.koitharu.kotatsu.domain.history.OnHistoryChangeListener
import org.koitharu.kotatsu.ui.widget.recent.RecentWidgetProvider
import org.koitharu.kotatsu.ui.widget.shelf.ShelfWidgetProvider

class WidgetUpdater(private val context: Context) : OnFavouritesChangeListener,
	OnHistoryChangeListener {

	override fun onFavouritesChanged(mangaId: Long) {
		updateWidget(ShelfWidgetProvider::class.java)
	}

	override fun onHistoryChanged() {
		updateWidget(RecentWidgetProvider::class.java)
	}

	private fun updateWidget(cls: Class<*>) {
		val intent = Intent(context, cls)
		intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		val ids = AppWidgetManager.getInstance(context)
			.getAppWidgetIds(ComponentName(context, cls))
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		context.sendBroadcast(intent)
	}
}