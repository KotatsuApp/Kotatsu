package org.koitharu.kotatsu.core.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.CallSuper
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig

abstract class BaseAppWidgetProvider : AppWidgetProvider() {

	@CallSuper
	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		appWidgetIds.forEach { id ->
			val config = AppWidgetConfig(context, javaClass, id)
			val views = onUpdateWidget(context, config)
			appWidgetManager.updateAppWidget(id, views)
		}
	}

	override fun onDeleted(context: Context, appWidgetIds: IntArray) {
		super.onDeleted(context, appWidgetIds)
		for (id in appWidgetIds) {
			AppWidgetConfig(context, javaClass, id).clear()
		}
	}

	override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
		super.onRestored(context, oldWidgetIds, newWidgetIds)
		if (oldWidgetIds.size != newWidgetIds.size) {
			return
		}
		for (i in oldWidgetIds.indices) {
			val oldId = oldWidgetIds[i]
			val newId = newWidgetIds[i]
			val oldConfig = AppWidgetConfig(context, javaClass, oldId)
			val newConfig = AppWidgetConfig(context, javaClass, newId)
			newConfig.copyFrom(oldConfig)
			oldConfig.clear()
		}
	}

	protected abstract fun onUpdateWidget(
		context: Context,
		config: AppWidgetConfig,
	): RemoteViews
}
