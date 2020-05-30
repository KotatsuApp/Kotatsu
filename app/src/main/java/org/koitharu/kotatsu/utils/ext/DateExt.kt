package org.koitharu.kotatsu.utils.ext

import android.annotation.SuppressLint
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)

fun Date.calendar(): Calendar = Calendar.getInstance().also {
	it.time = this
}

fun Date.formatRelative(minResolution: Long): CharSequence = DateUtils.getRelativeTimeSpanString(
	time, System.currentTimeMillis(), minResolution
)