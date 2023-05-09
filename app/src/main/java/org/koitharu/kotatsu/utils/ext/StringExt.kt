package org.koitharu.kotatsu.utils.ext

import androidx.annotation.FloatRange
import org.koitharu.kotatsu.parsers.util.levenshteinDistance
import java.util.UUID

inline fun String?.ifNullOrEmpty(defaultValue: () -> String): String {
	return if (this.isNullOrEmpty()) defaultValue() else this
}

fun String.longHashCode(): Long {
	var h = 1125899906842597L
	val len: Int = this.length
	for (i in 0 until len) {
		h = 31 * h + this[i].code
	}
	return h
}

fun String.toUUIDOrNull(): UUID? = try {
	UUID.fromString(this)
} catch (e: IllegalArgumentException) {
	e.printStackTraceDebug()
	null
}

/**
 * @param threshold 0 = exact match
 */
fun String.almostEquals(other: String, @FloatRange(from = 0.0) threshold: Float): Boolean {
	if (threshold == 0f) {
		return equals(other)
	}
	val diff = levenshteinDistance(other) / ((length + other.length) / 2f)
	return diff < threshold
}
