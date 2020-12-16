package org.koitharu.kotatsu.reader.ui.pager

data class ReaderUiState(
	val mangaName: String?,
	val chapterName: String?,
	val chapterNumber: Int,
	val chaptersTotal: Int
)