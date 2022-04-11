package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.parsers.model.Manga

data class MangaListModel(
	override val id: Long,
	val title: String,
	val subtitle: String,
	val coverUrl: String,
	override val manga: Manga,
	val counter: Int,
) : MangaItemModel