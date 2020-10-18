package org.koitharu.kotatsu.utils.ext

import androidx.collection.ArraySet

fun <T> MutableCollection<T>.replaceWith(subject: Iterable<T>) {
	clear()
	addAll(subject)
}

inline fun <T> Array<out T>.sumByLong(selector: (T) -> Long): Long {
	var sum = 0L
	for (element in this) {
		sum += selector(element)
	}
	return sum
}

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
	var sum = 0L
	for (element in this) {
		sum += selector(element)
	}
	return sum
}

fun <T> List<T>.medianOrNull(): T? = when {
	isEmpty() -> null
	else -> get((size / 2).coerceIn(indices))
}

inline fun <T, R> Collection<T>.mapToSet(transform: (T) -> R): Set<R> {
	val destination = ArraySet<R>(size)
	for (item in this) {
		destination.add(transform(item))
	}
	return destination
}

inline fun <T, R> Collection<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> {
	val destination = ArraySet<R>(size)
	for (item in this) {
		destination.add(transform(item) ?: continue)
	}
	return destination
}

fun LongArray.toArraySet(): Set<Long> {
	return when (size) {
		0 -> emptySet()
		1 -> setOf(this[0])
		else -> ArraySet<Long>(size).also { set ->
			for (item in this) {
				set.add(item)
			}
		}
	}
}