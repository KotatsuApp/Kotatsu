package org.koitharu.kotatsu.utils.ext

import androidx.collection.ArraySet
import java.util.*

fun <T> MutableCollection<T>.replaceWith(subject: Iterable<T>) {
	clear()
	addAll(subject)
}

fun <T> List<T>.medianOrNull(): T? = when {
	isEmpty() -> null
	else -> get((size / 2).coerceIn(indices))
}

inline fun <T, R> Collection<T>.mapToSet(transform: (T) -> R): Set<R> {
	return mapTo(ArraySet(size), transform)
}

fun LongArray.toArraySet(): Set<Long> = createSet(size) { i -> this[i] }

fun <T : Enum<T>> Array<T>.names() = Array(size) { i ->
	this[i].name
}

fun <T> Collection<T>.isDistinct(): Boolean {
	val set = HashSet<T>(size)
	for (item in this) {
		if (!set.add(item)) {
			return false
		}
	}
	return set.size == size
}

fun <T, K> Collection<T>.isDistinctBy(selector: (T) -> K): Boolean {
	val set = HashSet<K>(size)
	for (item in this) {
		if (!set.add(selector(item))) {
			return false
		}
	}
	return set.size == size
}

fun <T> MutableList<T>.move(sourceIndex: Int, targetIndex: Int) {
	if (sourceIndex <= targetIndex) {
		Collections.rotate(subList(sourceIndex, targetIndex + 1), -1)
	} else {
		Collections.rotate(subList(targetIndex, sourceIndex + 1), 1)
	}
}

inline fun <T> List<T>.areItemsEquals(other: List<T>, equals: (T, T) -> Boolean): Boolean {
	if (size != other.size) {
		return false
	}
	for (i in indices) {
		val a = this[i]
		val b = other[i]
		if (!equals(a, b)) {
			return false
		}
	}
	return true
}

@Suppress("FunctionName")
inline fun <T> MutableSet(size: Int, init: (index: Int) -> T): MutableSet<T> {
	val set = ArraySet<T>(size)
	repeat(size) { index -> set.add(init(index)) }
	return set
}

inline fun <T> createSet(size: Int, init: (index: Int) -> T): Set<T> = when (size) {
	0 -> emptySet()
	1 -> Collections.singleton(init(0))
	else -> MutableSet(size, init)
}

inline fun <T> createList(size: Int, init: (index: Int) -> T): List<T> = when (size) {
	0 -> emptyList()
	1 -> Collections.singletonList(init(0))
	else -> MutableList(size, init)
}