package org.koitharu.kotatsu.utils.ext

import androidx.core.os.LocaleListCompat
import java.util.*

fun LocaleListCompat.toList(): List<Locale> {
	val list = ArrayList<Locale>(size())
	for (i in 0 until size()) {
		list += get(i)
	}
	return list
}

inline fun <R, C : MutableCollection<in R>> LocaleListCompat.mapTo(
	destination: C,
	block: (Locale) -> R,
): C {
	val len = size()
	for (i in 0 until len) {
		val item = get(i)
		destination.add(block(item))
	}
	return destination
}

inline fun <T> LocaleListCompat.map(block: (Locale) -> T): List<T> {
	return mapTo(ArrayList(size()), block)
}

inline fun <T> LocaleListCompat.mapToSet(block: (Locale) -> T): Set<T> {
	return mapTo(LinkedHashSet(size()), block)
}