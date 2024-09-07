package org.koitharu.kotatsu.download.domain

data class DownloadProgress(
	val totalChapters: Int,
	val currentChapter: Int,
	val totalPages: Int,
	val currentPage: Int,
)
