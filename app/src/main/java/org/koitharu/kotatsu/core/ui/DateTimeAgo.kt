package org.koitharu.kotatsu.core.ui

import android.content.res.Resources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.daysDiff
import java.util.*
import java.util.concurrent.TimeUnit

sealed class DateTimeAgo {

	abstract fun format(resources: Resources): String

	object JustNow : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.just_now)
		}
	}

	data class MinutesAgo(val minutes: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.minutes_ago, minutes, minutes)
		}
	}

	data class HoursAgo(val hours: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.hours_ago, hours, hours)
		}
	}

	object Yesterday : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.yesterday)
		}
	}

	data class DaysAgo(val days: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.days_ago, days, days)
		}
	}

	object LongAgo : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.long_ago)
		}
	}

	companion object {

		fun from(date: Date): DateTimeAgo {
			val diff = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
			val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
			val diffHours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
			val diffDays = -date.daysDiff(System.currentTimeMillis())
			return when {
				diffMinutes < 1 -> JustNow
				diffMinutes < 60 -> MinutesAgo(diffMinutes)
				diffDays < 1 -> HoursAgo(diffHours)
				diffDays == 1 -> Yesterday
				diffDays < 16 -> DaysAgo(diffDays)
				else -> LongAgo
			}
		}
	}
}
