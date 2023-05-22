package org.koitharu.kotatsu.core.util

fun interface BufferedObserver<T> {

	fun onChanged(t: T, previous: T?)
}
