package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.parsers.model.Manga

data class HistoryInfo(
	val totalChapters: Int,
	val currentChapter: Int,
	val history: MangaHistory?,
	val isIncognitoMode: Boolean,
) {
	val isValid: Boolean
		get() = totalChapters >= 0
}

fun HistoryInfo(
	manga: Manga?,
	branch: String?,
	history: MangaHistory?,
	isIncognitoMode: Boolean
): HistoryInfo {
	val chapters = manga?.getChapters(branch)
	return HistoryInfo(
		totalChapters = chapters?.size ?: -1,
		currentChapter = if (history != null && !chapters.isNullOrEmpty()) {
			chapters.indexOfFirst { it.id == history.chapterId }
		} else {
			-1
		},
		history = history,
		isIncognitoMode = isIncognitoMode,
	)
}
