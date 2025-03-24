package org.koitharu.kotatsu.core.util.ext

import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.collection.LongSet
import org.koitharu.kotatsu.BuildConfig
import java.util.EnumSet

fun <T> Collection<T>.asArrayList(): ArrayList<T> = if (this is ArrayList<*>) {
	this as ArrayList<T>
} else {
	ArrayList(this)
}

fun <E : Enum<E>> Set<E>.asEnumSet(cls: Class<E>): EnumSet<E> = if (this is EnumSet<*>) {
	this as EnumSet<E>
} else {
	EnumSet.noneOf(cls).apply { addAll(this@asEnumSet) }
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

fun LongSet.toLongArray(): LongArray {
	val result = LongArray(size)
	var i = 0
	forEach { result[i++] = it }
	return result
}

fun LongSet.toSet(): Set<Long> = toCollection(ArraySet(size))

fun <R : MutableCollection<Long>> LongSet.toCollection(out: R): R = out.also { result ->
	forEach(result::add)
}

fun <T, R> Collection<T>.mapSortedByCount(isDescending: Boolean = true, mapper: (T) -> R): List<R> {
	val grouped = groupBy(mapper).toList()
	val sortSelector: (Pair<R, List<T>>) -> Int = { it.second.size }
	val sorted = if (isDescending) {
		grouped.sortedByDescending(sortSelector)
	} else {
		grouped.sortedBy(sortSelector)
	}
	return sorted.map { it.first }
}

fun Collection<CharSequence?>.contains(element: CharSequence?, ignoreCase: Boolean): Boolean = any { x ->
	(x == null && element == null) || (x != null && element != null && x.contains(element, ignoreCase))
}

fun Collection<CharSequence?>.indexOfContains(element: CharSequence?, ignoreCase: Boolean): Int = indexOfFirst { x ->
	(x == null && element == null) || (x != null && element != null && x.contains(element, ignoreCase))
}
