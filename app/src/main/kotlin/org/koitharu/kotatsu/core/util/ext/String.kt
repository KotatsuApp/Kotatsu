package org.koitharu.kotatsu.core.util.ext

import android.content.Context
import androidx.annotation.FloatRange
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.ellipsize
import org.koitharu.kotatsu.parsers.util.levenshteinDistance
import java.util.UUID

inline fun <C : CharSequence?> C?.ifNullOrEmpty(defaultValue: () -> C): C {
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
		return equals(other, ignoreCase = true)
	}
	val diff = lowercase().levenshteinDistance(other.lowercase()) / ((length + other.length) / 2f)
	return diff < threshold
}

fun CharSequence.sanitize(): CharSequence {
	return filterNot { c -> c.isReplacement() }
}

fun Char.isReplacement() = this in '\uFFF0'..'\uFFFF'

fun <T> Collection<T>.joinToStringWithLimit(context: Context, limit: Int, transform: ((T) -> String)): String {
	if (size == 1) {
		return transform(first()).ellipsize(limit)
	}
	return buildString(limit + 6) {
		for ((i, item) in this@joinToStringWithLimit.withIndex()) {
			val str = transform(item)
			when {
				i == 0 -> append(str.ellipsize(limit - 4))
				length + str.length > limit -> {
					append(", ")
					append(context.getString(R.string.list_ellipsize_pattern, this@joinToStringWithLimit.size - i))
					break
				}

				else -> append(", ").append(str)
			}
		}
	}
}
