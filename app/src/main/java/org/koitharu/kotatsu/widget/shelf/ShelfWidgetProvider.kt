package org.koitharu.kotatsu.widget.shelf

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.utils.PendingIntentCompat

class ShelfWidgetProvider : AppWidgetProvider() {

	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		appWidgetIds.forEach { id ->
			val views = RemoteViews(context.packageName, R.layout.widget_shelf)
			val adapter = Intent(context, ShelfWidgetService::class.java)
			adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
			adapter.data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME))
			views.setRemoteAdapter(R.id.gridView, adapter)
			val intent = Intent(context, ReaderActivity::class.java)
			intent.action = ReaderActivity.ACTION_MANGA_READ
			views.setPendingIntentTemplate(
				R.id.gridView, PendingIntent.getActivity(
					context,
					0,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
				)
			)
			views.setEmptyView(R.id.gridView, R.id.textView_holder)
			appWidgetManager.updateAppWidget(id, views)
		}
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.gridView)
	}
}