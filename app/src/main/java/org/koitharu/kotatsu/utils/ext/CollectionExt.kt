package org.koitharu.kotatsu.utils.ext

import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.collection.LongSparseArray
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

fun <K, V> List<Pair<K, V>>.toMutableMap(): MutableMap<K, V> = toMap(ArrayMap(size))

inline fun <T> Collection<T>.associateByLong(selector: (T) -> Long): LongSparseArray<T> {
	val result = LongSparseArray<T>(size)
	for (item in this) {
		result.put(selector(item), item)
	}
	return result
}

inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> = Array(size) { i ->
	transform(get(i))
}

fun <T : Enum<T>> Array<T>.names() = mapToArray { it.name }

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