package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.parsers.model.Manga

data class MangaCompactListModel(
	override val id: Long,
	override val title: String,
	val subtitle: String,
	override val coverUrl: String?,
	override val manga: Manga,
	override val counter: Int,
) : MangaListModel()
