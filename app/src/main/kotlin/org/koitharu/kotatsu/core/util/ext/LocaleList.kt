package org.koitharu.kotatsu.core.util.ext

import android.content.Context
import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.toTitleCase
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

fun String.toLocale() = Locale(this)

fun Locale?.getDisplayName(context: Context): String = when (this) {
	null -> context.getString(R.string.all_languages)
	Locale.ROOT -> context.getString(R.string.various_languages)
	else -> getDisplayLanguage(this).toTitleCase(this)
}

private class LocaleListCompatIterator(private val list: LocaleListCompat) : ListIterator<Locale> {

	private var index = 0

	override fun hasNext() = index < list.size()

	override fun hasPrevious() = index > 0

	override fun next() = list.get(index++) ?: throw NoSuchElementException()

	override fun nextIndex() = index

	override fun previous() = list.get(--index) ?: throw NoSuchElementException()

	override fun previousIndex() = index - 1
}
