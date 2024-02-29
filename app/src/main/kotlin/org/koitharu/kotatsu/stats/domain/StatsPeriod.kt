package org.koitharu.kotatsu.stats.domain

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class StatsPeriod(
	@StringRes val titleResId: Int,
	val days: Int,
) {

	DAY(R.string.day, 1),
	WEEK(R.string.week, 7),
	MONTH(R.string.month, 30),
	MONTHS_3(R.string.three_months, 90),
	ALL(R.string.all_time, Int.MAX_VALUE),
}
