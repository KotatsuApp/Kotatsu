package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import org.koitharu.kotatsu.utils.delegates.prefs.LongPreferenceDelegate

class AppWidgetConfig private constructor(
	private val prefs: SharedPreferences,
	val widgetId: Int
) :	SharedPreferences by prefs {

	var categoryId by LongPreferenceDelegate(CATEGORY_ID, 0L)

	companion object {

		private const val CATEGORY_ID = "cat_id"

		fun getInstance(context: Context, widgetId: Int) = AppWidgetConfig(
			context.getSharedPreferences(
				"appwidget_$widgetId",
				Context.MODE_PRIVATE
			), widgetId
		)
	}

}