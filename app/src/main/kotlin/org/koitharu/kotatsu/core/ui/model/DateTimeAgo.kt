package org.koitharu.kotatsu.core.ui.model

import android.content.res.Resources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.daysDiff
import org.koitharu.kotatsu.core.util.ext.format
import java.util.Date

sealed class DateTimeAgo {

	abstract fun format(resources: Resources): String

	object JustNow : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.just_now)
		}

		override fun toString() = "just_now"

		override fun equals(other: Any?): Boolean = other === JustNow
	}

	data class MinutesAgo(val minutes: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.minutes_ago, minutes, minutes)
		}

		override fun toString() = "minutes_ago_$minutes"
	}

	data class HoursAgo(val hours: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.hours_ago, hours, hours)
		}

		override fun toString() = "hours_ago_$hours"
	}

	object Today : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.today)
		}

		override fun toString() = "today"

		override fun equals(other: Any?): Boolean = other === Today
	}

	object Yesterday : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.yesterday)
		}

		override fun toString() = "yesterday"

		override fun equals(other: Any?): Boolean = other === Yesterday
	}

	data class DaysAgo(val days: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.days_ago, days, days)
		}

		override fun toString() = "days_ago_$days"
	}

	data class MonthsAgo(val months: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return if (months == 0) {
				resources.getString(R.string.this_month)
			} else {
				resources.getQuantityString(R.plurals.months_ago, months, months)
			}
		}
	}

	class Absolute(private val date: Date) : DateTimeAgo() {

		private val day = date.daysDiff(0)

		override fun format(resources: Resources): String {
			return date.format("d MMMM")
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Absolute

			return day == other.day
		}

		override fun hashCode(): Int {
			return day
		}

		override fun toString() = "abs_$day"
	}

	object LongAgo : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.long_ago)
		}

		override fun toString() = "long_ago"

		override fun equals(other: Any?): Boolean = other === LongAgo
	}
}
