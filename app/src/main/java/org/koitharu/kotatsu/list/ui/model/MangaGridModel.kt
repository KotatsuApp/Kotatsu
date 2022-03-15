package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.parsers.model.Manga

data class MangaGridModel(
	val id: Long,
	val title: String,
	val coverUrl: String,
	val manga: Manga,
	val counter: Int,
) : ListModel