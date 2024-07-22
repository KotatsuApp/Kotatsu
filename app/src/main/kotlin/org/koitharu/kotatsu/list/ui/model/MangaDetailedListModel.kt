package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.list.domain.ReadingProgress
import org.koitharu.kotatsu.parsers.model.Manga

data class MangaDetailedListModel(
	override val id: Long,
	override val title: String,
	val subtitle: String?,
	override val coverUrl: String,
	override val manga: Manga,
	override val counter: Int,
	override val progress: ReadingProgress?,
	override val isFavorite: Boolean,
	val tags: List<ChipsView.ChipModel>,
) : MangaListModel()
