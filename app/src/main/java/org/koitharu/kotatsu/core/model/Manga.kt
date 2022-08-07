package org.koitharu.kotatsu.core.model

import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.utils.ext.iterator

fun Collection<Manga>.ids() = mapToSet { it.id }

fun Manga.getPreferredBranch(history: MangaHistory?): String? {
	val ch = chapters
	if (ch.isNullOrEmpty()) {
		return null
	}
	if (history != null) {
		val currentChapter = ch.find { it.id == history.chapterId }
		if (currentChapter != null) {
			return currentChapter.branch
		}
	}
	val groups = ch.groupBy { it.branch }
	for (locale in LocaleListCompat.getAdjustedDefault()) {
		var language = locale.getDisplayLanguage(locale).toTitleCase(locale)
		if (groups.containsKey(language)) {
			return language
		}
		language = locale.getDisplayName(locale).toTitleCase(locale)
		if (groups.containsKey(language)) {
			return language
		}
	}
	return groups.maxByOrNull { it.value.size }?.key
}