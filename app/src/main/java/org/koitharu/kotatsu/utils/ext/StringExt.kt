package org.koitharu.kotatsu.utils.ext

fun String.longHashCode(): Long {
	var h = 1125899906842597L
	val len: Int = this.length
	for (i in 0 until len) {
		h = 31 * h + this[i].toLong()
	}
	return h
}