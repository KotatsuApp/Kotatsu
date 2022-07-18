package org.koitharu.kotatsu.utils.ext

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