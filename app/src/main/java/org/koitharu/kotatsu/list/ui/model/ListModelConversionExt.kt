package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.core.model.Manga
import kotlin.math.roundToInt

fun Manga.toListModel() = MangaListModel(
	id = id,
	title = title,
	subtitle = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this
)

fun Manga.toListDetailedModel() = MangaListDetailedModel(
	id = id,
	title = title,
	subtitle = altTitle,
	rating = "${(rating * 10).roundToInt()}/10",
	tags = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this
)

fun Manga.toGridModel() = MangaGridModel(
	id = id,
	title = title,
	coverUrl = coverUrl,
	manga = this
)