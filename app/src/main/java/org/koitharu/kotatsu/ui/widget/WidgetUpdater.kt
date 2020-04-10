package org.koitharu.kotatsu.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.koitharu.kotatsu.domain.favourites.OnFavouritesChangeListener
import org.koitharu.kotatsu.ui.widget.shelf.ShelfWidgetProvider

class WidgetUpdater(private val context: Context) : OnFavouritesChangeListener {

	override fun onFavouritesChanged(mangaId: Long) {
		val intent = Intent(context, ShelfWidgetProvider::class.java)
		intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		val ids = AppWidgetManager.getInstance(context)
			.getAppWidgetIds(ComponentName(context, ShelfWidgetProvider::class.java))
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		context.sendBroadcast(intent)
	}

}