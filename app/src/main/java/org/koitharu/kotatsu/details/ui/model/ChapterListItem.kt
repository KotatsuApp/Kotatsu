package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.history.domain.ChapterExtra

data class ChapterListItem(
	val chapter: MangaChapter,
	val extra: ChapterExtra,
	val isMissing: Boolean,
	val uploadDate: String?,
) {

	fun description(): CharSequence? {
		val scanlator = chapter.scanlator?.takeUnless { it.isBlank() }
		return when {
			uploadDate != null && scanlator != null -> "$uploadDate • $scanlator"
			scanlator != null -> scanlator
			else -> uploadDate
		}
	}
}
