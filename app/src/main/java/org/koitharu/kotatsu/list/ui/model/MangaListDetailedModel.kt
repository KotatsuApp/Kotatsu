package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.core.model.Manga

data class MangaListDetailedModel(
	val id: Long,
	val title: String,
	val subtitle: String?,
	val tags: String,
	val coverUrl: String,
	val rating: String?,
	val manga: Manga
)