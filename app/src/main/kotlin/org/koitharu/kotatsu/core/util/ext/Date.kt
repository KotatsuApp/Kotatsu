package org.koitharu.kotatsu.core.util.ext

import android.annotation.SuppressLint
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("SimpleDateFormat")
fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)

fun Date.daysDiff(other: Long): Int {
	val thisDay = time / TimeUnit.DAYS.toMillis(1L)
	val otherDay = other / TimeUnit.DAYS.toMillis(1L)
	return (thisDay - otherDay).toInt()
}
