package org.koitharu.kotatsu.ui.reader.base

import java.util.*

class GroupedList<K, T> {

	private val data = LinkedList<Pair<K, List<T>>>()

	private var intSize: Int = -1
	private var lruGroup: List<T>? = null
	private var lruGroupKey: K? = null
	private var lruGroupFirstIndex = -1

	val size: Int
		get() {
			if (intSize < 0) {
				computeSize()
			}
			return intSize
		}

	val groupCount: Int
		get() = data.size

	val isEmpty: Boolean
		get() = size == 0

	val isNotEmpty: Boolean
		get() = size != 0

	operator fun get(index: Int): T {
		if (index >= lruGroupFirstIndex) {
			val relIndex = index - lruGroupFirstIndex
			lruGroup?.let {
				if (relIndex in it.indices) {
					return it[relIndex]
				}
			}
		}
		if (intSize < 0 || index < intSize shr 1) {
			var firstIndex = 0
			for (entry in data.iterator()) {
				if (index < firstIndex + entry.second.size && index >= firstIndex) {
					lruGroup = entry.second
					lruGroupKey = entry.first
					lruGroupFirstIndex = firstIndex
					return entry.second[index - firstIndex]
				}
				firstIndex += entry.second.size
			}
		} else {
			var lastIndex = intSize
			for (entry in data.descendingIterator()) {
				if (index < lastIndex && index >= lastIndex - entry.second.size) {
					lruGroup = entry.second
					lruGroupKey = entry.first
					lruGroupFirstIndex = lastIndex - entry.second.size
					return entry.second[index - lruGroupFirstIndex]
				}
				lastIndex -= entry.second.size
			}
		}
		throw IndexOutOfBoundsException()
	}

	fun getOrNull(index: Int) = try {
		get(index)
	} catch (e: IndexOutOfBoundsException) {
		null
	}

	fun getLastKey() = data.peekLast()?.first

	fun getFirstKey() = data.peekFirst()?.first

	fun getGroup(key: K): List<T>? {
		if (key == lruGroupKey && lruGroup != null) {
			return lruGroup
		} else {
			for(entry in data) {
				if (entry.first == key) {
					return entry.second
				}
			}
		}
		return null
	}

	fun getRelativeIndex(absIndex: Int): Int {
		if (absIndex >= lruGroupFirstIndex) {
			val relIndex = absIndex - lruGroupFirstIndex
			lruGroup?.let {
				if (relIndex in it.indices) {
					return relIndex
				}
			}
		}
		if (intSize < 0 || absIndex < intSize shr 1) {
			var firstIndex = 0
			for (entry in data.iterator()) {
				if (absIndex < firstIndex + entry.second.size && absIndex >= firstIndex) {
					 return absIndex - firstIndex
				}
				firstIndex += entry.second.size
			}
		} else {
			var lastIndex = intSize
			for (entry in data.descendingIterator()) {
				if (absIndex < lastIndex && absIndex >= lastIndex - entry.second.size) {
					return absIndex - lruGroupFirstIndex
				}
				lastIndex -= entry.second.size
			}
		}
		return -1
	}

	fun findGroupByIndex(absIndex: Int): K? {
		if (absIndex >= lruGroupFirstIndex && lruGroupKey != null) {
			val relIndex = absIndex - lruGroupFirstIndex
			lruGroup?.let {
				if (relIndex in it.indices) {
					return lruGroupKey
				}
			}
		}
		if (intSize < 0 || absIndex < intSize shr 1) {
			var firstIndex = 0
			for (entry in data.iterator()) {
				if (absIndex < firstIndex + entry.second.size && absIndex >= firstIndex) {
					return entry.first
				}
				firstIndex += entry.second.size
			}
		} else {
			var lastIndex = intSize
			for (entry in data.descendingIterator()) {
				if (absIndex < lastIndex && absIndex >= lastIndex - entry.second.size) {
					return entry.first
				}
				lastIndex -= entry.second.size
			}
		}
		return null
	}

	fun getGroupOffset(key: K): Int {
		if (lruGroupKey == key && lruGroupFirstIndex >= 0) {
			return lruGroupFirstIndex
		}
		var offset = 0
		for (entry in data) {
			if (entry.first == key) {
				return offset
			}
			offset += entry.second.size
		}
		return -1
	}

	fun indexOf(item: T): Int {
		var offset = 0
		for ((_, list) in data) {
			for ((i, x) in list.withIndex()) {
				if (x == item) {
					return i + offset
				}
			}
			offset += list.size
		}
		return -1
	}

	fun addLast(key: K, items: List<T>) {
		data.addLast(key to items.toList())
		if (intSize < 0) {
			computeSize()
		} else {
			intSize += items.size
		}
	}

	fun addFirst(key: K, items: List<T>) {
		data.addFirst(key to items.toList())
		if (lruGroupFirstIndex >= 0) {
			lruGroupFirstIndex += items.size
		}
		if (intSize < 0) {
			computeSize()
		} else {
			intSize += items.size
		}
	}

	fun removeLast(): List<T> {
		val item = data.removeLast()
		if (intSize < 0) {
			computeSize()
		} else {
			intSize -= item.second.size
		}
		return item.second
	}

	fun removeFirst(): List<T> {
		val item = data.removeFirst()
		if (intSize < 0) {
			computeSize()
		} else {
			intSize -= item.second.size
		}
		if (lruGroupFirstIndex >= 0) {
			lruGroupFirstIndex -= item.second.size
		}
		return item.second
	}

	fun clear() {
		data.clear()
		intSize = 0
		lruGroupFirstIndex = -1
		lruGroup = null
		lruGroupKey = null
	}

	private fun computeSize() {
		intSize = data.sumBy { it.second.size }
	}
}