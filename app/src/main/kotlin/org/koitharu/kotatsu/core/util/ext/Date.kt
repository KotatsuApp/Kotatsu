package org.koitharu.kotatsu.core.util.ext

import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun calculateTimeAgo(instant: Instant, showMonths: Boolean = false): DateTimeAgo {
	// TODO: Use Java 9's LocalDate.ofInstant().
	val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
	val now = LocalDate.now()
	val diffDays = localDate.until(now, ChronoUnit.DAYS)

	return when {
		diffDays == 0L -> {
			if (instant.until(Instant.now(), ChronoUnit.MINUTES) < 3) DateTimeAgo.JustNow
			else DateTimeAgo.Today
		}

		diffDays == 1L -> DateTimeAgo.Yesterday
		diffDays < 6 -> DateTimeAgo.DaysAgo(diffDays.toInt())
		else -> {
			val diffMonths = localDate.until(now, ChronoUnit.MONTHS)
			if (showMonths && diffMonths <= 6) {
				DateTimeAgo.MonthsAgo(diffMonths.toInt())
			} else {
				DateTimeAgo.Absolute(localDate)
			}
		}
	}
}

fun Long.toInstantOrNull() = if (this == 0L) null else Instant.ofEpochMilli(this)
