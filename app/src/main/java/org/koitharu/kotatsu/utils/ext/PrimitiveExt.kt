package org.koitharu.kotatsu.utils.ext

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

fun Number.format(decimals: Int = 0, decPoint: Char = '.', thousandsSep: Char? = ' '): String {
	val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
	val symbols = formatter.decimalFormatSymbols
	if (thousandsSep != null) {
		symbols.groupingSeparator = thousandsSep
		formatter.isGroupingUsed = true
	} else {
		formatter.isGroupingUsed = false
	}
	symbols.decimalSeparator = decPoint
	formatter.decimalFormatSymbols = symbols
	formatter.minimumFractionDigits = decimals
	formatter.maximumFractionDigits = decimals
	return when (this) {
		is Float,
		is Double -> formatter.format(this.toDouble())
		else -> formatter.format(this.toLong())
	}
}

inline fun Int.ifZero(defaultValue: () -> Int): Int = if (this == 0) defaultValue() else this