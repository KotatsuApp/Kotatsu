package org.koitharu.kotatsu.ui.reader

import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.common.BaseMvpView

interface ReaderListener : BaseMvpView {

	fun onPageChanged(chapter: MangaChapter, page: Int, total: Int)

	fun saveState(chapterId: Long, page: Int)
}