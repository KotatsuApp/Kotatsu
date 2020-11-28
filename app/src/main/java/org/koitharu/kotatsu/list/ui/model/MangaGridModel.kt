package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.core.model.Manga

data class MangaGridModel(
	val id: Long,
	val title: String,
	val coverUrl: String,
	val manga: Manga
)