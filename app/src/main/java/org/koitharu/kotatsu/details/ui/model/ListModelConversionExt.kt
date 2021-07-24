package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.history.domain.ChapterExtra

fun MangaChapter.toListItem(
	extra: ChapterExtra,
	isMissing: Boolean,
) = ChapterListItem(
	chapter = this,
	extra = extra,
	isMissing = isMissing,
)