package org.koitharu.kotatsu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.ext.processLifecycleScope
import org.koitharu.kotatsu.widget.recent.RecentWidgetProvider
import org.koitharu.kotatsu.widget.shelf.ShelfWidgetProvider

class WidgetUpdater(private val context: Context) {

	fun subscribeToFavourites(repository: FavouritesRepository) {
		repository.observeAll(SortOrder.NEWEST)
			.onEach { updateWidget(ShelfWidgetProvider::class.java) }
			.retry { error -> error !is CancellationException }
			.launchIn(processLifecycleScope + Dispatchers.Default)
	}

	fun subscribeToHistory(repository: HistoryRepository) {
		repository.observeAll()
			.onEach { updateWidget(RecentWidgetProvider::class.java) }
			.retry { error -> error !is CancellationException }
			.launchIn(processLifecycleScope + Dispatchers.Default)
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