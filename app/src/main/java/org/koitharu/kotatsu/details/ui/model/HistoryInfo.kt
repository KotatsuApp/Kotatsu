package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.parsers.model.Manga

class HistoryInfo(
	val totalChapters: Int,
	val currentChapter: Int,
	val history: MangaHistory?,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as HistoryInfo

		if (totalChapters != other.totalChapters) return false
		if (currentChapter != other.currentChapter) return false
		if (history != other.history) return false

		return true
	}

	override fun hashCode(): Int {
		var result = totalChapters
		result = 31 * result + currentChapter
		result = 31 * result + (history?.hashCode() ?: 0)
		return result
	}
}

@Suppress("FunctionName")
fun HistoryInfo(manga: Manga?, history: MangaHistory?): HistoryInfo? {
	val chapters = manga?.chapters ?: return null
	return HistoryInfo(
		totalChapters = chapters.size,
		currentChapter = if (history != null) {
			chapters.indexOfFirst { it.id == history.chapterId }
		} else {
			-1
		},
		history = history,
	)
}
