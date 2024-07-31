package org.koitharu.kotatsu.core.util.ext

import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.collection.LongSet
import org.koitharu.kotatsu.BuildConfig
import java.util.Collections
import java.util.EnumSet

inline fun <T> MutableSet(size: Int, init: (index: Int) -> T): MutableSet<T> {
	val set = ArraySet<T>(size)
	repeat(size) { index -> set.add(init(index)) }
	return set
}

inline fun <T> Set(size: Int, init: (index: Int) -> T): Set<T> = when (size) {
	0 -> emptySet()
	1 -> Collections.singleton(init(0))
	else -> MutableSet(size, init)
}

fun <T> Collection<T>.asArrayList(): ArrayList<T> = if (this is ArrayList<*>) {
	this as ArrayList<T>
} else {
	ArrayList(this)
}

fun <K, V> Map<K, V>.findKeyByValue(value: V): K? {
	for ((k, v) in entries) {
		if (v == value) {
			return k
		}
	}
	return null
}

fun <T> Sequence<T>.toListSorted(comparator: Comparator<T>): List<T> {
	return toMutableList().apply { sortWith(comparator) }
}

fun <T> List<T>.takeMostFrequent(limit: Int): List<T> {
	val map = ArrayMap<T, Int>(size)
	for (item in this) {
		map[item] = map.getOrDefault(item, 0) + 1
	}
	val entries = map.entries.sortedByDescending { it.value }
	val count = minOf(limit, entries.size)
	return buildList(count) {
		repeat(count) { i ->
			add(entries[i].key)
		}
	}
}

inline fun <reified E : Enum<E>> Collection<E>.toEnumSet(): EnumSet<E> = if (isEmpty()) {
	EnumSet.noneOf(E::class.java)
} else {
	EnumSet.copyOf(this)
}

fun <E : Enum<E>> Collection<E>.sortedByOrdinal() = sortedBy { it.ordinal }

fun <T> Iterable<T>.sortedWithSafe(comparator: Comparator<in T>): List<T> = try {
	sortedWith(comparator)
} catch (e: IllegalArgumentException) {
	if (BuildConfig.DEBUG) {
		throw e
	} else {
		toList()
	}
}

fun Collection<*>?.sizeOrZero() = this?.size ?: 0

@Suppress("UNCHECKED_CAST")
inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
	val result = arrayOfNulls<R>(size)
	forEachIndexed { index, t -> result[index] = transform(t) }
	return result as Array<R>
}

fun LongSet.toLongArray(): LongArray {
	val result = LongArray(size)
	var i = 0
	forEach { result[i++] = it }
	return result
}

fun LongSet.toSet(): Set<Long> = toCollection(ArraySet<Long>(size))

fun <R : MutableCollection<Long>> LongSet.toCollection(out: R): R = out.also { result ->
	forEach(result::add)
}
