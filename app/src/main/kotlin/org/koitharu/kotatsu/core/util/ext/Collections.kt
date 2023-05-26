package org.koitharu.kotatsu.core.util.ext

import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import java.util.Collections

@Deprecated("TODO: remove")
fun <T> MutableList<T>.move(sourceIndex: Int, targetIndex: Int) {
	if (sourceIndex <= targetIndex) {
		Collections.rotate(subList(sourceIndex, targetIndex + 1), -1)
	} else {
		Collections.rotate(subList(targetIndex, sourceIndex + 1), 1)
	}
}

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

inline fun <T> Collection<T>.filterToSet(predicate: (T) -> Boolean): Set<T> {
	return filterTo(ArraySet(size), predicate)
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
