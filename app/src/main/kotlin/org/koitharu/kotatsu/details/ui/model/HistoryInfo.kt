package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.parsers.model.Manga

data class HistoryInfo(
	val totalChapters: Int,
	val currentChapter: Int,
	val history: MangaHistory?,
	val isIncognitoMode: Boolean,
	val isChapterMissing: Boolean,
	val canDownload: Boolean,
) {
	val isValid: Boolean
		get() = totalChapters >= 0

	val canContinue: Boolean
		get() = history != null && !isChapterMissing
}

fun HistoryInfo(
	manga: MangaDetails?,
	branch: String?,
	history: MangaHistory?,
	isIncognitoMode: Boolean
): HistoryInfo {
	val chapters = manga?.chapters?.get(branch)
	val currentChapter = if (history != null && !chapters.isNullOrEmpty()) {
		chapters.indexOfFirst { it.id == history.chapterId }
	} else {
		-2
	}
	return HistoryInfo(
		totalChapters = chapters?.size ?: -1,
		currentChapter = currentChapter,
		history = history,
		isIncognitoMode = isIncognitoMode,
		isChapterMissing = currentChapter == -1,
		canDownload = manga?.isLocal == false,
	)
}
