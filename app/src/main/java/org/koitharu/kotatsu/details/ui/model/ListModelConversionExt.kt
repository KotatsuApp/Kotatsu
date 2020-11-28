package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.history.domain.ChapterExtra

fun MangaChapter.toListItem(extra: ChapterExtra) = ChapterListItem(
	chapter = this,
	extra = extra
)