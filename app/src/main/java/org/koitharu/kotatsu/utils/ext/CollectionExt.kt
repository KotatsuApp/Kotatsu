package org.koitharu.kotatsu.utils.ext

fun <T> MutableCollection<T>.replaceWith(subject: Iterable<T>) {
	clear()
	addAll(subject)
}