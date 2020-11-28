package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.history.domain.ChapterExtra

data class ChapterListItem(
	val chapter: MangaChapter,
	val extra: ChapterExtra
)
