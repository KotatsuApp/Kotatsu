package org.koitharu.kotatsu.details.data

import android.content.res.Resources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getQuantityStringSafe

data class ReadingTime(
	val minutes: Int,
	val hours: Int,
	val isContinue: Boolean,
) {

	fun format(resources: Resources): String = when {
		hours == 0 && minutes == 0 -> resources.getString(R.string.less_than_minute)
		hours == 0 -> resources.getQuantityStringSafe(R.plurals.minutes, minutes, minutes)
		minutes == 0 -> resources.getQuantityStringSafe(R.plurals.hours, hours, hours)
		else -> resources.getString(
			R.string.remaining_time_pattern,
			resources.getQuantityStringSafe(R.plurals.hours, hours, hours),
			resources.getQuantityStringSafe(R.plurals.minutes, minutes, minutes),
		)
	}

	fun formatShort(resources: Resources): String? = when {
		hours == 0 && minutes == 0 -> null
		hours == 0 -> resources.getString(R.string.minutes_short, minutes)
		minutes == 0 -> resources.getString(R.string.hours_short, hours)
		else -> resources.getString(R.string.hours_minutes_short, hours, minutes)
	}
}
