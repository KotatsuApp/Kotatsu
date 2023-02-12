package org.koitharu.kotatsu.utils.ext

import androidx.collection.ArraySet
import java.util.Collections

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
