package org.koitharu.kotatsu.core.model

import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.core.util.ext.iterator
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet

fun Collection<Manga>.ids() = mapToSet { it.id }

fun Collection<Manga>.distinctById() = distinctBy { it.id }

fun Collection<ChapterListItem>.countChaptersByBranch(): Int {
	if (size <= 1) {
		return size
	}
	val acc = HashMap<String?, Int>()
	for (item in this) {
		val branch = item.chapter.branch
		acc[branch] = (acc[branch] ?: 0) + 1
	}
	return acc.values.max()
}

fun Manga.findChapter(id: Long): MangaChapter? {
	return chapters?.find { it.id == id }
}

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
	if (groups.size == 1) {
		return groups.keys.first()
	}
	val candidates = HashMap<String?, List<MangaChapter>>(groups.size)
	for (locale in LocaleListCompat.getAdjustedDefault()) {
		val displayLanguage = locale.getDisplayLanguage(locale)
		val displayName = locale.getDisplayName(locale)
		for (branch in groups.keys) {
			if (branch != null && (
					branch.contains(displayLanguage, ignoreCase = true) ||
						branch.contains(displayName, ignoreCase = true)
					)
			) {
				candidates[branch] = groups[branch] ?: continue
			}
		}
	}
	return candidates.ifEmpty { groups }.maxByOrNull { it.value.size }?.key
}

val Manga.isLocal: Boolean
	get() = source == MangaSource.LOCAL
