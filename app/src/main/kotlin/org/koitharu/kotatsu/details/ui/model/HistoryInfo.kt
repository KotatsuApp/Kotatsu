package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.details.data.MangaDetails

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

	val canContinue
		get() = currentChapter >= 0

	val percent: Float
		get() = if (history != null && (canContinue || isChapterMissing)) {
			history.percent
		} else {
			0f
		}
}

fun HistoryInfo(
	manga: MangaDetails?,
	branch: String?,
	history: MangaHistory?,
	isIncognitoMode: Boolean
): HistoryInfo {
	val chapters = if (manga?.chapters?.isEmpty() == true) {
		emptyList()
	} else {
		manga?.chapters?.get(branch)
	}
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
		isChapterMissing = history != null && manga?.isLoaded == true && manga.allChapters.none { it.id == history.chapterId },
		canDownload = manga?.isLocal == false,
	)
}
