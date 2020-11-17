package org.koitharu.kotatsu.reader.ui

import org.koitharu.kotatsu.core.model.MangaChapter

interface ReaderListener {

	fun onPageChanged(chapter: MangaChapter, page: Int)

	fun saveState(chapterId: Long, page: Int, scroll: Int)

	fun onLoadingStateChanged(isLoading: Boolean)

	fun onError(error: Throwable)
}