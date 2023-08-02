package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.parsers.model.Manga

data class MangaListModel(
	override val id: Long,
	override val title: String,
	val subtitle: String,
	override val coverUrl: String,
	override val manga: Manga,
	override val counter: Int,
	override val progress: Float,
) : MangaItemModel()
