package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.parsers.model.Manga

sealed interface MangaItemModel : ListModel {

	val id: Long
	val manga: Manga
	val title: String
	val coverUrl: String
	val counter: Int
	val progress: Float
}