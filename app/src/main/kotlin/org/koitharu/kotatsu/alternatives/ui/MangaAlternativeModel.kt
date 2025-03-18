package org.koitharu.kotatsu.alternatives.ui

import org.koitharu.kotatsu.core.model.chaptersCount
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.parsers.model.Manga

data class MangaAlternativeModel(
	val mangaModel: MangaGridModel,
	private val referenceChapters: Int,
) : ListModel {

	val manga: Manga
		get() = mangaModel.manga

	val chaptersCount = manga.chaptersCount()

	val chaptersDiff: Int
		get() = if (referenceChapters == 0 || chaptersCount == 0) 0 else chaptersCount - referenceChapters

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaAlternativeModel && other.manga.id == manga.id
	}

	override fun getChangePayload(previousState: ListModel): Any? = if (previousState is MangaAlternativeModel) {
		mangaModel.getChangePayload(previousState.mangaModel)
	} else {
		null
	}
}
