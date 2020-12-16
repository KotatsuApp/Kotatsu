package org.koitharu.kotatsu.utils

fun interface BufferedObserver<T> {

	fun onChanged(t: T, previous: T?)
}