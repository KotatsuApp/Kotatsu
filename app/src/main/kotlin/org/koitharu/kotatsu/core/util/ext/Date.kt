package org.koitharu.kotatsu.core.util.ext

import android.annotation.SuppressLint
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@SuppressLint("SimpleDateFormat")
fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)

fun calculateTimeAgo(date: Date, showDate: Boolean = true): DateTimeAgo {
	val localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
	val now = LocalDateTime.now()
	val diffMinutes = localDateTime.until(now, ChronoUnit.MINUTES)
	val diffDays = localDateTime.until(now, ChronoUnit.DAYS).toInt()
	return when {
		diffMinutes < 3 -> DateTimeAgo.JustNow
		diffDays < 1 -> DateTimeAgo.Today
		diffDays == 1 -> DateTimeAgo.Yesterday
		diffDays < 6 -> DateTimeAgo.DaysAgo(diffDays)
		else -> if (showDate) DateTimeAgo.Absolute(localDateTime.toLocalDate()) else DateTimeAgo.LongAgo
	}
}

fun Date.startOfDay(): Long {
	val calendar = Calendar.getInstance()
	calendar.time = this
	calendar[Calendar.HOUR_OF_DAY] = 0
	calendar[Calendar.MINUTE] = 0
	calendar[Calendar.SECOND] = 0
	calendar[Calendar.MILLISECOND] = 0
	return calendar.timeInMillis
}
