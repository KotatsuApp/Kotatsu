package org.koitharu.kotatsu.reader.ui.pager

data class ReaderUiState(
	val mangaName: String?,
	val chapterName: String?,
	val chapterNumber: Int,
	val chaptersTotal: Int,
	val currentPage: Int,
	val totalPages: Int,
) {

	fun computePercent(): Float {
		val ppc = 1f / chaptersTotal
		val chapterIndex = chapterNumber - 1
		val pagePercent = (currentPage + 1) / totalPages.toFloat()
		return ppc * chapterIndex + ppc * pagePercent
	}
}
