package org.koitharu.kotatsu.settings.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

class ActivityListPreference : ListPreference {

	var activityIntent: Intent? = null

	constructor(
		context: Context,
		attrs: AttributeSet?,
		defStyleAttr: Int,
		defStyleRes: Int
	) : super(context, attrs, defStyleAttr, defStyleRes)

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	constructor(context: Context) : super(context)

	override fun onClick() {
		val intent = activityIntent
		if (intent == null) {
			super.onClick()
			return
		}
		try {
			context.startActivity(intent)
		} catch (e: ActivityNotFoundException) {
			e.printStackTraceDebug()
			super.onClick()
		}
	}
}
