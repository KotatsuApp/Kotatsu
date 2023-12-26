package org.koitharu.kotatsu.core.util

import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.core.util.ext.map
import java.util.Locale

class LocaleComparator : Comparator<Locale> {

	private val deviceLocales = LocaleListCompat.getAdjustedDefault()//LocaleManagerCompat.getSystemLocales(context)
		.map { it.language }
		.distinct()

	override fun compare(a: Locale, b: Locale): Int {
		val indexA = deviceLocales.indexOf(a.language)
		val indexB = deviceLocales.indexOf(b.language)
		return when {
			indexA < 0 && indexB < 0 -> compareValues(a.language, b.language)
			indexA < 0 -> 1
			indexB < 0 -> -1
			else -> compareValues(indexA, indexB)
		}
	}
}
