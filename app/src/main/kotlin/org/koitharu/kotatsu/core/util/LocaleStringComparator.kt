package org.koitharu.kotatsu.core.util

import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.core.util.ext.indexOfContains
import org.koitharu.kotatsu.core.util.ext.iterator

class LocaleStringComparator : Comparator<String?> {

	private val deviceLocales: List<String?>

	init {
		val localeList = LocaleListCompat.getAdjustedDefault()
		deviceLocales = buildList(localeList.size() + 1) {
			add(null)
			val set = HashSet<String?>(localeList.size() + 1)
			set.add(null)
			for (locale in localeList) {
				val lang = locale.getDisplayLanguage(locale)
				if (set.add(lang)) {
					add(lang)
				}
			}
		}
	}

	override fun compare(a: String?, b: String?): Int {
		val indexA = deviceLocales.indexOfContains(a, true)
		val indexB = deviceLocales.indexOfContains(b, true)
		return when {
			indexA < 0 && indexB < 0 -> compareValues(a, b)
			indexA < 0 -> 1
			indexB < 0 -> -1
			else -> compareValues(indexA, indexB)
		}
	}
}
