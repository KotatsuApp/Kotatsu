package org.koitharu.kotatsu.utils.ext

import android.annotation.SuppressLint
import org.intellij.lang.annotations.PrintFormat
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)

fun Date.calendar(): Calendar = Calendar.getInstance().also {
	it.time = this
}