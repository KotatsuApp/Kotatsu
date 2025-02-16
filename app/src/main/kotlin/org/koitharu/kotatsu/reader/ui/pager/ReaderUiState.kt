package org.koitharu.kotatsu.reader.ui.pager

data class ReaderUiState(
	val mangaName: String?,
	val branch: String?,
	val chapterName: String?,
	val chapterNumber: Int,
	val chaptersTotal: Int,
	val currentPage: Int,
	val totalPages: Int,
	val percent: Float,
	val incognito: Boolean,
) {

	fun hasNextChapter(): Boolean = chapterNumber < chaptersTotal

	fun hasPreviousChapter(): Boolean = chapterNumber > 1

	fun isSliderAvailable(): Boolean = totalPages > 1 && currentPage < totalPages
}
