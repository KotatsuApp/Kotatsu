package org.koitharu.kotatsu.reader.ui.pager

data class ReaderUiState(
	val mangaName: String?,
	val chapterName: String?,
	val chapterNumber: Int,
	val chaptersTotal: Int,
	val currentPage: Int,
	val totalPages: Int,
	val percent: Float,
	private val isSliderEnabled: Boolean,
) {

	fun isSliderAvailable(): Boolean {
		return isSliderEnabled && totalPages > 1 && currentPage < totalPages
	}
}
