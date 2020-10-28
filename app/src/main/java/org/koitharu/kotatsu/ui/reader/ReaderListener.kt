package org.koitharu.kotatsu.ui.reader

import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.base.BaseMvpView

interface ReaderListener : BaseMvpView {

	fun onPageChanged(chapter: MangaChapter, page: Int)

	fun saveState(chapterId: Long, page: Int, scroll: Int)
}