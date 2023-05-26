package org.koitharu.kotatsu.core.model

import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.toTitleCase
import java.util.Locale

fun MangaSource.getLocaleTitle(): String? {
	val lc = Locale(locale ?: return null)
	return lc.getDisplayLanguage(lc).toTitleCase(lc)
}

fun MangaSource(name: String): MangaSource {
	MangaSource.values().forEach {
		if (it.name == name) return it
	}
	return MangaSource.DUMMY
}
