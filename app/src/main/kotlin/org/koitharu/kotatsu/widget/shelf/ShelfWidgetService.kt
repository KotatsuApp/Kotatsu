package org.koitharu.kotatsu.widget.shelf

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository

@AndroidEntryPoint
class ShelfWidgetService : RemoteViewsService() {

	@Inject
	lateinit var favouritesRepository: FavouritesRepository

	@Inject
	lateinit var coil: ImageLoader

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		val widgetId = intent.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID,
		)
		return ShelfListFactory(applicationContext, favouritesRepository, coil, widgetId)
	}
}
