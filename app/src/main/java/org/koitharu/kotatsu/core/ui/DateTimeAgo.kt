package org.koitharu.kotatsu.core.ui

import android.content.res.Resources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.daysDiff
import org.koitharu.kotatsu.utils.ext.format
import java.util.*

sealed class DateTimeAgo : ListModel {

	abstract fun format(resources: Resources): String

	object JustNow : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.just_now)
		}
	}

	class MinutesAgo(val minutes: Int) : DateTimeAgo() {

		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.minutes_ago, minutes, minutes)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as MinutesAgo
			return minutes == other.minutes
		}

		override fun hashCode(): Int = minutes
	}

	class HoursAgo(val hours: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.hours_ago, hours, hours)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as HoursAgo
			return hours == other.hours
		}

		override fun hashCode(): Int = hours
	}

	object Today : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.today)
		}
	}

	object Yesterday : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.yesterday)
		}
	}

	class DaysAgo(val days: Int) : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getQuantityString(R.plurals.days_ago, days, days)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as DaysAgo
			return days == other.days
		}

		override fun hashCode(): Int = days
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

			if (day != other.day) return false

			return true
		}

		override fun hashCode(): Int {
			return day
		}
	}

	object LongAgo : DateTimeAgo() {
		override fun format(resources: Resources): String {
			return resources.getString(R.string.long_ago)
		}
	}
}