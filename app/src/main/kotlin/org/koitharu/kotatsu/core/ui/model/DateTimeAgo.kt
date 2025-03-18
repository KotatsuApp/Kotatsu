package org.koitharu.kotatsu.core.ui.model

import android.content.Context
import android.text.format.DateUtils
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getQuantityStringSafe
import org.koitharu.kotatsu.core.util.ext.toMillis
import java.time.LocalDate

sealed class DateTimeAgo {

	abstract fun format(context: Context): String

	object JustNow : DateTimeAgo() {
		override fun format(context: Context): String {
			return context.getString(R.string.just_now)
		}

		override fun toString() = "just_now"

		override fun equals(other: Any?): Boolean = other === JustNow
	}

	data class MinutesAgo(val minutes: Int) : DateTimeAgo() {
		override fun format(context: Context): String {
			return context.resources.getQuantityStringSafe(
				R.plurals.minutes_ago,
				minutes,
				minutes,
			)
		}

		override fun toString() = "minutes_ago_$minutes"
	}

	data class HoursAgo(val hours: Int) : DateTimeAgo() {
		override fun format(context: Context): String {
			return context.resources.getQuantityStringSafe(
				R.plurals.hours_ago,
				hours,
				hours,
			)
		}

		override fun toString() = "hours_ago_$hours"
	}

	object Today : DateTimeAgo() {
		override fun format(context: Context): String {
			return context.getString(R.string.today)
		}

		override fun toString() = "today"

		override fun equals(other: Any?): Boolean = other === Today
	}

	object Yesterday : DateTimeAgo() {
		override fun format(context: Context): String {
			return context.getString(R.string.yesterday)
		}

		override fun toString() = "yesterday"

		override fun equals(other: Any?): Boolean = other === Yesterday
	}

	data class DaysAgo(val days: Int) : DateTimeAgo() {
		override fun format(context: Context): String {
			return context.resources.getQuantityStringSafe(R.plurals.days_ago, days, days)
		}

		override fun toString() = "days_ago_$days"
	}

	data class MonthsAgo(val months: Int) : DateTimeAgo() {
		override fun format(context: Context): String {
			return if (months == 0) {
				context.getString(R.string.this_month)
			} else {
				context.resources.getQuantityStringSafe(
					R.plurals.months_ago,
					months,
					months,
				)
			}
		}
	}

	data class Absolute(private val date: LocalDate) : DateTimeAgo() {
		override fun format(context: Context): String {
			return if (date == EPOCH_DATE) {
				context.getString(R.string.unknown)
			} else {
				DateUtils.formatDateTime(context, date.toMillis(), DateUtils.FORMAT_SHOW_DATE)
			}
		}

		override fun toString() = "abs_${date.toEpochDay()}"

		private companion object {
			val EPOCH_DATE: LocalDate = LocalDate.of(1970, 1, 1)
		}
	}

	object LongAgo : DateTimeAgo() {
		override fun format(context: Context): String {
			return context.getString(R.string.long_ago)
		}

		override fun toString() = "long_ago"

		override fun equals(other: Any?): Boolean = other === LongAgo
	}
}
