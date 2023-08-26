package org.koitharu.kotatsu.core.prefs

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import androidx.core.content.edit

private const val CATEGORY_ID = "cat_id"
private const val BACKGROUND = "bg"

class AppWidgetConfig(
	context: Context,
	cls: Class<out AppWidgetProvider>,
	val widgetId: Int,
) {

	private val prefs = context.getSharedPreferences("appwidget_${cls.simpleName}_$widgetId", Context.MODE_PRIVATE)

	var categoryId: Long
		get() = prefs.getLong(CATEGORY_ID, 0L)
		set(value) = prefs.edit { putLong(CATEGORY_ID, value) }

	var hasBackground: Boolean
		get() = prefs.getBoolean(BACKGROUND, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		set(value) = prefs.edit { putBoolean(BACKGROUND, value) }

	fun clear() {
		prefs.edit { clear() }
	}

	fun copyFrom(other: AppWidgetConfig) {
		prefs.edit {
			clear()
			putLong(CATEGORY_ID, other.categoryId)
			putBoolean(BACKGROUND, other.hasBackground)
		}
	}
}
