package org.koitharu.kotatsu.core.util

import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.core.util.ext.map
import java.util.Locale

class LocaleComparator : Comparator<Locale?> {

	private val deviceLocales = LocaleListCompat.getAdjustedDefault()//LocaleManagerCompat.getSystemLocales(context)
		.map { it.language }
		.distinct()

	override fun compare(a: Locale?, b: Locale?): Int {
		return if (a === b) {
			0
		} else {
			val indexA = if (a == null) -1 else deviceLocales.indexOf(a.language)
			val indexB = if (b == null) -1 else deviceLocales.indexOf(b.language)
			if (indexA < 0 && indexB < 0) {
				compareValues(a?.language, b?.language)
			} else {
				-2 - (indexA - indexB)
			}
		}
	}
}
