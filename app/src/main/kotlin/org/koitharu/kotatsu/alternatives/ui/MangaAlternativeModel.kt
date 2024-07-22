package org.koitharu.kotatsu.alternatives.ui

import org.koitharu.kotatsu.core.model.chaptersCount
import org.koitharu.kotatsu.list.domain.ReadingProgress
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

data class MangaAlternativeModel(
	val manga: Manga,
	val progress: ReadingProgress?,
	private val referenceChapters: Int,
) : ListModel {

	val chaptersCount = manga.chaptersCount()

	val chaptersDiff: Int
		get() = if (referenceChapters == 0 || chaptersCount == 0) 0 else chaptersCount - referenceChapters

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaAlternativeModel && other.manga.id == manga.id
	}
}
