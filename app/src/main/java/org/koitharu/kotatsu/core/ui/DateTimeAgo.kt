package org.koitharu.kotatsu.core.ui

import android.content.res.Resources
import org.koitharu.kotatsu.R

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
}
