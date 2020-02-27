package org.koitharu.kotatsu.ui.reader

import org.koitharu.kotatsu.core.model.MangaChapter

interface ReaderListener {

	fun onPageChanged(chapter: MangaChapter, page: Int, total: Int)
}