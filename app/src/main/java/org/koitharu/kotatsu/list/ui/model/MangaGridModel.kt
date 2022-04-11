package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.parsers.model.Manga

data class MangaGridModel(
	override val id: Long,
	val title: String,
	val coverUrl: String,
	override val manga: Manga,
	val counter: Int,
) : MangaItemModel