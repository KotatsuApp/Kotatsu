package org.koitharu.kotatsu.reader.ui.pager

import android.content.res.Resources
import org.koitharu.kotatsu.core.model.getLocalizedTitle
import org.koitharu.kotatsu.parsers.model.MangaChapter

data class ReaderUiState(
	val mangaName: String?,
	val chapter: MangaChapter,
	val chapterIndex: Int,
	val chaptersTotal: Int,
	val currentPage: Int,
	val totalPages: Int,
	val percent: Float,
	val incognito: Boolean,
	val hasFallbackNext: Boolean = false,
	val hasFallbackPrevious: Boolean = false,
) {

	val chapterNumber: Int
		get() = chapterIndex + 1

	fun hasNextChapter(): Boolean = chapterNumber < chaptersTotal || hasFallbackNext

	fun hasPreviousChapter(): Boolean = chapterIndex > 0 || hasFallbackPrevious

	fun isSliderAvailable(): Boolean = totalPages > 1 && currentPage < totalPages

	fun getChapterTitle(resources: Resources) = chapter.getLocalizedTitle(resources, chapterIndex)
}
