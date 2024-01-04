package org.koitharu.kotatsu.reader.domain

import androidx.collection.LongSparseArray
import androidx.collection.contains
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

class ChapterPages private constructor(private val pages: ArrayDeque<ReaderPage>) : List<ReaderPage> by pages {

	// map chapterId to index in pages deque
	private val indices = LongSparseArray<IntRange>()

	constructor() : this(ArrayDeque())

	val chaptersSize: Int
		get() = indices.size()

	fun removeFirst() {
		val chapterId = pages.first().chapterId
		indices.remove(chapterId)
		var delta = 0
		while (pages.first().chapterId == chapterId) {
			pages.removeFirst()
			delta--
		}
		shiftIndices(delta)
	}

	fun removeLast() {
		val chapterId = pages.last().chapterId
		indices.remove(chapterId)
		while (pages.last().chapterId == chapterId) {
			pages.removeLast()
		}
	}

	fun addLast(id: Long, newPages: List<ReaderPage>) {
		indices.put(id, pages.size until (pages.size + newPages.size))
		pages.addAll(newPages)
	}

	fun addFirst(id: Long, newPages: List<ReaderPage>) {
		shiftIndices(newPages.size)
		indices.put(id, newPages.indices)
		pages.addAll(0, newPages)
	}

	fun clear() {
		indices.clear()
		pages.clear()
	}

	fun size(id: Long) = indices[id]?.run {
		endInclusive - start + 1
	} ?: 0

	fun subList(id: Long): List<ReaderPage> {
		val range = indices[id] ?: return emptyList()
		return pages.subList(range.first, range.last + 1)
	}

	operator fun contains(chapterId: Long)  = indices.contains(chapterId)

	private fun shiftIndices(delta: Int) {
		for (i in 0 until indices.size()) {
			val range = indices.valueAt(i)
			indices.setValueAt(i, range + delta)
		}
	}

	private operator fun IntRange.plus(delta: Int): IntRange {
		return IntRange(start + delta, endInclusive + delta)
	}
}
