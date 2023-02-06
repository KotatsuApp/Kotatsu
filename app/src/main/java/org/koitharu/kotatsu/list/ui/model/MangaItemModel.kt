package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface MangaItemModel : ListModel {

	val id: Long
	val manga: Manga
	val title: String
	val coverUrl: String
	val counter: Int
	val progress: Float

	val source: MangaSource
		get() = manga.source
}
