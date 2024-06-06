package org.koitharu.kotatsu.core.util

import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.core.util.ext.iterator
import java.util.Locale

class LocaleComparator : Comparator<Locale> {

	private val deviceLocales: List<String>

	init {
		val localeList = LocaleListCompat.getAdjustedDefault()
		deviceLocales = buildList(localeList.size() + 1) {
			add("")
			val set = HashSet<String>(localeList.size() + 1)
			set.add("")
			for (locale in localeList) {
				val lang = locale.language
				if (set.add(lang)) {
					add(lang)
				}
			}
		}
	}

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
