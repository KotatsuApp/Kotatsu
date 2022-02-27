package org.koitharu.kotatsu.core.prefs

import android.content.Context
import androidx.core.content.edit

private const val CATEGORY_ID = "cat_id"

class AppWidgetConfig(context: Context, val widgetId: Int) {

	private val prefs = context.getSharedPreferences("appwidget_$widgetId", Context.MODE_PRIVATE)

	var categoryId: Long
		get() = prefs.getLong(CATEGORY_ID, 0L)
		set(value) = prefs.edit { putLong(CATEGORY_ID, value) }
}
