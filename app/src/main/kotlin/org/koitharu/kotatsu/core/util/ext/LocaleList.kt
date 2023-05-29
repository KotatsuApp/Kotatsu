package org.koitharu.kotatsu.core.util.ext

import androidx.core.os.LocaleListCompat
import java.util.Locale

operator fun LocaleListCompat.iterator(): ListIterator<Locale> = LocaleListCompatIterator(this)

fun LocaleListCompat.toList(): List<Locale> = List(size()) { i -> getOrThrow(i) }

inline fun <T> LocaleListCompat.map(block: (Locale) -> T): List<T> {
	return List(size()) { i -> block(getOrThrow(i)) }
}

inline fun <T> LocaleListCompat.mapToSet(block: (Locale) -> T): Set<T> {
	return Set(size()) { i -> block(getOrThrow(i)) }
}

fun LocaleListCompat.getOrThrow(index: Int) = get(index) ?: throw NoSuchElementException()

private class LocaleListCompatIterator(private val list: LocaleListCompat) : ListIterator<Locale> {

	private var index = 0

	override fun hasNext() = index < list.size()

	override fun hasPrevious() = index > 0

	override fun next() = list.get(index++) ?: throw NoSuchElementException()

	override fun nextIndex() = index

	override fun previous() = list.get(--index) ?: throw NoSuchElementException()

	override fun previousIndex() = index - 1
}
