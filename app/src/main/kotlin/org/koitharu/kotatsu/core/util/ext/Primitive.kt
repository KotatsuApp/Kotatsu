package org.koitharu.kotatsu.core.util.ext

inline fun Int.ifZero(defaultValue: () -> Int): Int = if (this == 0) defaultValue() else this

fun longOf(a: Int, b: Int): Long {
	return a.toLong() shl 32 or (b.toLong() and 0xffffffffL)
}
