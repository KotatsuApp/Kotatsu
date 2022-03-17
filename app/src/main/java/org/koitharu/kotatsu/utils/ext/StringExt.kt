package org.koitharu.kotatsu.utils.ext

inline fun String?.ifNullOrEmpty(defaultValue: () -> String): String {
	return if (this.isNullOrEmpty()) defaultValue() else this
}