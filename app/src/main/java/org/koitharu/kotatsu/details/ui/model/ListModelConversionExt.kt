package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.history.domain.ChapterExtra
import java.text.DateFormat

fun MangaChapter.toListItem(
	extra: ChapterExtra,
	isMissing: Boolean,
	dateFormat: DateFormat,
) = ChapterListItem(
	chapter = this,
	extra = extra,
	isMissing = isMissing,
	uploadDate = if (uploadDate != 0L) dateFormat.format(uploadDate) else null
)