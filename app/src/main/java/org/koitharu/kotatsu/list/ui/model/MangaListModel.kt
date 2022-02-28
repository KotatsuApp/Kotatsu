package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.core.model.Manga

data class MangaListModel(
	val id: Long,
	val title: String,
	val subtitle: String,
	val coverUrl: String,
	val manga: Manga,
	val counter: Int,
) : ListModel