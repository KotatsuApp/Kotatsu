package org.koitharu.kotatsu.widget.shelf

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig
import org.koitharu.kotatsu.core.ui.BaseAppWidgetProvider
import org.koitharu.kotatsu.reader.ui.ReaderActivity

class ShelfWidgetProvider : BaseAppWidgetProvider() {

	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		super.onUpdate(context, appWidgetManager, appWidgetIds)
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.gridView)
	}

	override fun onUpdateWidget(context: Context, config: AppWidgetConfig): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_shelf)
		if (!config.hasBackground) {
			views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
		} else {
			views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_appwidget_root)
		}
		val adapter = Intent(context, ShelfWidgetService::class.java)
		adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
		adapter.data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME))
		views.setRemoteAdapter(R.id.gridView, adapter)
		val intent = Intent(context, ReaderActivity::class.java)
		intent.action = ReaderActivity.ACTION_MANGA_READ
		views.setPendingIntentTemplate(
			R.id.gridView,
			PendingIntentCompat.getActivity(
				context,
				0,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT,
				true,
			),
		)
		views.setEmptyView(R.id.gridView, R.id.textView_holder)
		return views
	}
}
