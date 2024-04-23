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
	private val isSliderEnabled: Boolean,
) {

	fun isSliderAvailable(): Boolean {
		return isSliderEnabled && totalPages > 1 && currentPage < totalPages
	}
}
