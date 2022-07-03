package org.koitharu.kotatsu.utils.ext

import androidx.collection.ArraySet
import java.util.*

fun <T> MutableList<T>.move(sourceIndex: Int, targetIndex: Int) {
	if (sourceIndex <= targetIndex) {
		Collections.rotate(subList(sourceIndex, targetIndex + 1), -1)
	} else {
		Collections.rotate(subList(targetIndex, sourceIndex + 1), 1)
	}
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

fun <T> List<T>.asArrayList(): ArrayList<T> = if (this is ArrayList<*>) {
	this as ArrayList<T>
} else {
	ArrayList(this)
}

fun <K, V> Map<K, V>.findKey(value: V): K? {
	for ((k, v) in entries) {
		if (v == value) {
			return k
		}
	}
	return null
}